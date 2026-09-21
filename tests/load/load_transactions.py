#!/usr/bin/env python3
"""Carga HTTP reproducible para medir operaciones bancarias de SmartBancs.

El script usa solo la biblioteca estandar. Necesita un JWT y una cuenta validos.
Por defecto envia depositos de 0.01 USD con claves de idempotencia nuevas.
"""
from __future__ import annotations

import argparse
import json
import statistics
import threading
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass


@dataclass
class Result:
    status: int | None
    latency_ms: float
    error: str | None = None


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Mide el throughput del endpoint de transacciones")
    parser.add_argument("--url", default="http://localhost:8088/api/transactions/deposits")
    parser.add_argument("--token", required=True, help="JWT sin el prefijo Bearer")
    parser.add_argument("--account", required=True, help="Cuenta de ocho digitos del JWT")
    parser.add_argument("--rate", type=int, default=10000, help="Objetivo de solicitudes por segundo")
    parser.add_argument("--duration", type=float, default=10.0, help="Duracion en segundos")
    parser.add_argument("--workers", type=int, default=256, help="Solicitudes concurrentes maximas")
    parser.add_argument("--amount", default="0.01", help="Importe decimal por deposito")
    parser.add_argument("--timeout", type=float, default=10.0, help="Timeout por solicitud")
    parser.add_argument(
        "--allow-errors",
        action="store_true",
        help="Devuelve codigo 0 aunque haya respuestas no exitosas",
    )
    return parser.parse_args()


def send_one(args: argparse.Namespace) -> Result:
    body = json.dumps({"accountNumber": args.account, "amount": args.amount}).encode("utf-8")
    request = urllib.request.Request(
        args.url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {args.token}",
            "Content-Type": "application/json",
            "Idempotency-Key": str(uuid.uuid4()),
        },
    )
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=args.timeout) as response:
            response.read()
            status = response.status
        return Result(status, (time.perf_counter() - started) * 1000)
    except urllib.error.HTTPError as error:
        error.read()
        return Result(error.code, (time.perf_counter() - started) * 1000, f"HTTP {error.code}")
    except (urllib.error.URLError, TimeoutError, OSError) as error:
        return Result(None, (time.perf_counter() - started) * 1000, type(error).__name__)


def percentile(values: list[float], fraction: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    index = min(len(ordered) - 1, int((len(ordered) - 1) * fraction))
    return ordered[index]


def main() -> int:
    args = parse_args()
    if args.rate <= 0 or args.duration <= 0 or args.workers <= 0:
        raise SystemExit("rate, duration y workers deben ser mayores que cero")

    planned = int(args.rate * args.duration)
    results: list[Result] = []
    started = time.perf_counter()
    next_release = started

    print(f"Objetivo: {args.rate} TPS durante {args.duration:.1f}s ({planned} solicitudes)")
    print(f"Workers: {args.workers} | Endpoint: {args.url}")
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = []
        for _ in range(planned):
            now = time.perf_counter()
            if now < next_release:
                time.sleep(next_release - now)
            futures.append(executor.submit(send_one, args))
            next_release += 1 / args.rate
        results = [future.result() for future in futures]

    elapsed = time.perf_counter() - started
    successful = [item for item in results if item.status is not None and 200 <= item.status < 300]
    errors = [item for item in results if item not in successful]
    latencies = [item.latency_ms for item in results]
    status_counts: dict[str, int] = {}
    for item in results:
        key = str(item.status) if item.status is not None else "network-error"
        status_counts[key] = status_counts.get(key, 0) + 1

    print("\nResultado")
    print(f"Solicitudes enviadas: {len(results)}")
    print(f"Respuestas 2xx: {len(successful)}")
    print(f"Errores: {len(errors)}")
    print(f"Throughput efectivo: {len(results) / elapsed:.2f} req/s")
    print(f"Latencia min/p50/p95/p99/max: {min(latencies):.2f}/{percentile(latencies, .50):.2f}/{percentile(latencies, .95):.2f}/{percentile(latencies, .99):.2f}/{max(latencies):.2f} ms")
    print(f"Estados: {status_counts}")

    if errors and not args.allow_errors:
        print("La prueba termino con errores. Usa --allow-errors solo para una medicion exploratoria.")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
