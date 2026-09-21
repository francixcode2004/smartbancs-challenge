from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from typing import Any

from fastapi import FastAPI, Request
from fastapi.concurrency import run_in_threadpool
from openai import OpenAI

app = FastAPI(title="SmartBancs Recommendations", version="2.0.0")

MODEL = os.getenv("OPENAI_MODEL", "gpt-4o")
PROMPT_VERSION = "recommendations-v2"
MAX_INPUT_CHARS = 20000
VALID_PRIORITIES = {"low", "medium", "high"}
VALID_CATEGORIES = {"saving", "budget", "spending"}

api_key = os.getenv("OPENAI_API_KEY") or os.getenv("API_KEY")
client = OpenAI(api_key=api_key) if api_key else None


async def read_body(request: Request) -> Any:
    """Lee el body tal como venga: JSON si se puede, si no texto plano."""
    raw = await request.body()
    if not raw:
        return None
    try:
        return json.loads(raw)
    except ValueError:
        return raw.decode("utf-8", errors="replace")


def fallback() -> list[dict[str, str]]:
    return [
        {
            "title": "Revisa tu presupuesto",
            "message": "Compara tus ingresos con tus gastos del periodo y identifica las categorías donde más gastas.",
            "priority": "medium",
            "category": "budget",
        }
    ]


def build_prompt(data: Any) -> str:
    serialized = json.dumps(data, ensure_ascii=False, default=str)[:MAX_INPUT_CHARS]
    return f"""Eres un asistente de educación financiera para SmartBancs.
Analiza únicamente los siguientes datos, que pueden venir en cualquier formato:
{serialized}

Genera entre 1 y 3 recomendaciones educativas, claras y accionables.
No inventes datos, no recomiendes inversiones específicas, no prometas rendimientos,
no ejecutes transacciones y no solicites información personal.
Devuelve exclusivamente un JSON con esta forma:
{{"recommendations":[{{"title":"...","message":"...","priority":"low|medium|high","category":"saving|budget|spending"}}]}}"""


def clean_recommendation(item: Any) -> dict[str, str] | None:
    if not isinstance(item, dict):
        return None
    title = str(item.get("title", "")).strip()
    message = str(item.get("message", "")).strip()
    if not title or not message:
        return None
    priority = item.get("priority")
    category = item.get("category")
    return {
        "title": title,
        "message": message,
        "priority": priority if priority in VALID_PRIORITIES else "medium",
        "category": category if category in VALID_CATEGORIES else "budget",
    }


def openai_recommendations(data: Any) -> list[dict[str, str]]:
    response = client.chat.completions.create(
        model=MODEL,
        temperature=0.2,
        response_format={"type": "json_object"},
        messages=[
            {"role": "system", "content": "Devuelve únicamente JSON válido."},
            {"role": "user", "content": build_prompt(data)},
        ],
    )
    payload = json.loads(response.choices[0].message.content or "{}")
    items = payload.get("recommendations", [])
    cleaned = [clean_recommendation(item) for item in items]
    return [item for item in cleaned if item][:3]


def build_response(recommendations: list[dict[str, str]], source: str) -> dict[str, Any]:
    return {
        "recommendations": recommendations,
        "model": MODEL if source == "openai" else "rules-v1",
        "promptVersion": PROMPT_VERSION,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "source": source,
    }


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/recommendations")
async def recommendations(request: Request) -> dict[str, Any]:
    data = await read_body(request)

    if client is None or data in (None, "", {}, []):
        return build_response(fallback(), "fallback")

    try:
        result = await run_in_threadpool(openai_recommendations, data)
    except Exception:
        return build_response(fallback(), "fallback")

    if not result:
        return build_response(fallback(), "fallback")
    return build_response(result, "openai")