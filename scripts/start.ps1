[CmdletBinding()]
param([switch]$ConfigureOnly)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$composePath = Join-Path $projectRoot 'compose.yaml'
$envPath = Join-Path $projectRoot '.env'
$templatePath = Join-Path $projectRoot '.env.example'

if (!(Test-Path -LiteralPath $composePath) -or !(Test-Path -LiteralPath $templatePath)) {
    throw 'Ejecuta scripts/start.ps1 dentro del repositorio SmartBancs completo.'
}

if (!(Test-Path -LiteralPath $envPath)) {
    $settings = [IO.File]::ReadAllText($templatePath)
    $jwtBytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($jwtBytes) } finally { $random.Dispose() }
    $secret = [Convert]::ToBase64String($jwtBytes)
    $settings = [regex]::Replace($settings, '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$secret")
    [IO.File]::WriteAllText($envPath, $settings, (New-Object Text.UTF8Encoding($false)))
    Write-Host '.env creado con un secreto JWT aleatorio. No lo subas al repositorio.'
} else {
    Write-Host 'Se conserva el archivo .env existente.'
}

if ($ConfigureOnly) {
    Write-Host 'Configura OPENAI_API_KEY en .env para habilitar recomendaciones con IA.'
    Write-Host 'Despues ejecuta de nuevo scripts/start.ps1 sin -ConfigureOnly.'
    return
}

if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'No se encontro Docker. Instala Docker Desktop, abrelo y vuelve a ejecutar este script.'
}

Push-Location $projectRoot
try {
    & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Docker no responde. Abre Docker Desktop y espera a que inicie.' }
    & docker compose version
    if ($LASTEXITCODE -ne 0) { throw 'Se necesita Docker Compose v2.' }

    # --quiet valida sin imprimir claves ni valores resueltos de .env.
    & docker compose config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose no es valido. Revisa compose.yaml y .env.' }

    & docker compose up -d --build --wait --wait-timeout 240
    if ($LASTEXITCODE -ne 0) {
        throw 'El arranque no termino correctamente. Ejecuta: docker compose logs --tail=80'
    }

    # Un volumen existente no vuelve a ejecutar database/init; comprobamos las tablas del MVP.
    $schemaQuery = "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name IN ('users','accounts','basic_services','transactions','recommendations','bancs_outbox');"
    $tableCount = & docker compose exec -T postgres psql -U smartbancs -d smartbancs -v ON_ERROR_STOP=1 -tAc $schemaQuery
    if ($LASTEXITCODE -ne 0 -or (($tableCount -join '').Trim() -ne '6')) {
        throw 'Faltan tablas del MVP. Consulta la seccion Base de datos del README; no se borraron datos.'
    }

    $health = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8088/api/health' -TimeoutSec 15
    if ($health.StatusCode -ne 200 -or $health.Content.Trim() -ne 'OK') {
        throw 'Nginx no pudo confirmar la salud de la API.'
    }
    & docker compose ps
    Write-Host ''
    Write-Host 'MVP listo: http://localhost:8088'
    Write-Host 'Prometheus: http://localhost:9090 | Grafana: http://localhost:3000'
    Write-Host 'Crea tus usuarios desde Registro y sigue la prueba del README.'
    Write-Host 'Las recomendaciones con movimientos requieren una clave OpenAI valida en .env.'
    Write-Host 'Para detener sin borrar datos: docker compose down'
} finally {
    Pop-Location
}
