"""Transform a raw transaction batch into clean rows and customer features.

Usage:
    python etl/transform_transactions.py
    python etl/transform_transactions.py --input etl/input/transactions_raw.csv
"""

from __future__ import annotations

import argparse
import csv
import json
from collections import defaultdict
from datetime import datetime, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path

TYPE_ALIASES = {
    "credit": "deposit",
    "deposit": "deposit",
    "debit": "withdraw",
    "withdrawal": "withdraw",
    "withdraw": "withdraw",
    "transfer": "transfer",
    "service-payment": "service_payment",
    "service_payment": "service_payment",
    "payment": "service_payment",
}

INCOME_TYPES = {"deposit"}
EXPENSE_TYPES = {"withdraw", "service_payment"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("etl/input/transactions_raw.csv"),
    )
    parser.add_argument(
        "--clean-output",
        type=Path,
        default=Path("etl/output/transactions_clean.csv"),
    )
    parser.add_argument(
        "--features-output",
        type=Path,
        default=Path("etl/output/customer_features.json"),
    )
    return parser.parse_args()


def parse_timestamp(value: str) -> datetime:
    normalized = value.strip().replace("Z", "+00:00")
    parsed = datetime.fromisoformat(normalized)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed.astimezone(timezone.utc)


def parse_amount(value: str) -> Decimal:
    normalized = value.strip().replace(" ", "")
    if "," in normalized and "." in normalized:
        normalized = normalized.replace(",", "")
    else:
        normalized = normalized.replace(",", ".")
    amount = Decimal(normalized)
    if amount <= 0:
        raise InvalidOperation
    return amount.quantize(Decimal("0.01"))


def clean_row(row: dict[str, str]) -> dict[str, str] | None:
    account_number = (row.get("account_number") or "").strip()
    transaction_id = (row.get("transaction_id") or "").strip()
    raw_type = (row.get("type") or "").strip().lower().replace(" ", "_")
    raw_direction = (row.get("direction") or "").strip().upper()
    description = " ".join((row.get("description") or "").split())

    if not transaction_id or not account_number.isdigit() or len(account_number) != 8:
        return None
    if raw_type not in TYPE_ALIASES:
        return None
    if raw_direction not in {"IN", "OUT"}:
        return None

    try:
        amount = parse_amount(row.get("amount") or "")
        created_at = parse_timestamp(row.get("created_at") or "")
    except (InvalidOperation, ValueError):
        return None

    normalized_type = TYPE_ALIASES[raw_type]
    if normalized_type in INCOME_TYPES:
        flow = "income"
    elif normalized_type in EXPENSE_TYPES or raw_direction == "OUT":
        flow = "expense"
    else:
        flow = "transfer"

    category = (row.get("category") or normalized_type).strip().lower()
    return {
        "transaction_id": transaction_id,
        "account_number": account_number,
        "created_at": created_at.isoformat().replace("+00:00", "Z"),
        "period": created_at.strftime("%Y-%m"),
        "type": normalized_type,
        "flow": flow,
        "category": category or normalized_type,
        "amount": format(amount, ".2f"),
        "description": description,
    }


def build_features(rows: list[dict[str, str]]) -> list[dict[str, object]]:
    groups: dict[tuple[str, str], dict[str, object]] = defaultdict(
        lambda: {
            "income": Decimal("0"),
            "expenses": Decimal("0"),
            "transaction_count": 0,
            "categories": defaultdict(Decimal),
        }
    )

    for row in rows:
        key = (row["account_number"], row["period"])
        group = groups[key]
        amount = Decimal(row["amount"])
        group["transaction_count"] += 1
        if row["flow"] == "income":
            group["income"] += amount
        elif row["flow"] == "expense":
            group["expenses"] += amount
            group["categories"][row["category"]] += amount

    features = []
    for (account_number, period), group in sorted(groups.items()):
        income = group["income"]
        expenses = group["expenses"]
        savings = income - expenses
        ratio = expenses / income if income else Decimal("0")
        categories = {
            name: float(amount.quantize(Decimal("0.01")))
            for name, amount in sorted(group["categories"].items())
        }
        features.append(
            {
                "account_number": account_number,
                "period": period,
                "income": float(income.quantize(Decimal("0.01"))),
                "expenses": float(expenses.quantize(Decimal("0.01"))),
                "savings": float(savings.quantize(Decimal("0.01"))),
                "expense_ratio": float(ratio.quantize(Decimal("0.0001"))),
                "transaction_count": group["transaction_count"],
                "categories": categories,
            }
        )
    return features


def run(input_path: Path, clean_output: Path, features_output: Path) -> tuple[int, int]:
    clean_rows = []
    with input_path.open(newline="", encoding="utf-8") as source:
        for row in csv.DictReader(source):
            cleaned = clean_row(row)
            if cleaned is not None:
                clean_rows.append(cleaned)

    clean_output.parent.mkdir(parents=True, exist_ok=True)
    features_output.parent.mkdir(parents=True, exist_ok=True)
    fieldnames = [
        "transaction_id", "account_number", "created_at", "period",
        "type", "flow", "category", "amount", "description",
    ]
    with clean_output.open("w", newline="", encoding="utf-8") as target:
        writer = csv.DictWriter(target, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(clean_rows)

    with features_output.open("w", encoding="utf-8") as target:
        json.dump(build_features(clean_rows), target, indent=2, ensure_ascii=True)
        target.write("\n")

    return len(clean_rows), len(build_features(clean_rows))


if __name__ == "__main__":
    args = parse_args()
    clean_count, feature_count = run(args.input, args.clean_output, args.features_output)
    print(f"ETL completado: {clean_count} transacciones limpias, {feature_count} grupos de features")
