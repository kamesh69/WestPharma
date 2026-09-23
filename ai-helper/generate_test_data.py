#!/usr/bin/env python3
"""Generate dynamic test data (and negative scenarios) for Web/API automation.

Uses OpenAI or Anthropic when an API key is present; otherwise falls back to a
deterministic offline generator so demos never block.
"""

from __future__ import annotations

import json
import os
import sys
import uuid
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent
RAW_DIR = ROOT / "artifacts" / "raw"
VALIDATED_DIR = ROOT / "artifacts" / "validated"
SCHEMA_PATH = ROOT / "schemas" / "test_data.schema.json"

PROMPT = """You are a test-data generator for an SDET framework.
Return ONLY valid JSON (no markdown) matching this shape:
{
  "source": "openai|anthropic",
  "model": "<model>",
  "generatedAt": "<ISO8601>",
  "employees": [
    {"firstName": "...", "middleName": "...", "lastName": "..."},
    {"firstName": "...", "middleName": "...", "lastName": "..."}
  ],
  "bookings": [
    {
      "firstname": "...",
      "lastname": "...",
      "totalprice": 123,
      "depositpaid": true,
      "bookingdates": {"checkin": "YYYY-MM-DD", "checkout": "YYYY-MM-DD"},
      "additionalneeds": "..."
    },
    { ... second booking ... }
  ],
  "negativeApiScenarios": [
    {"name": "...", "description": "...", "expectedStatus": 403},
    {"name": "...", "description": "...", "expectedStatus": 400},
    {"name": "...", "description": "...", "expectedStatus": 404}
  ]
}
Rules:
- Exactly 2 unique employees with realistic names (prefix AutoAI for uniqueness).
- Exactly 2 bookings; checkout must be after checkin; totalprice >= 0.
- At least 3 negative API scenarios covering invalid data and authorization.
"""


def _token(n: int = 6) -> str:
    return uuid.uuid4().hex[:n]


def offline_payload() -> dict:
    today = date.today()
    checkin1 = today + timedelta(days=3)
    checkout1 = checkin1 + timedelta(days=2)
    checkin2 = today + timedelta(days=5)
    checkout2 = checkin2 + timedelta(days=4)
    return {
        "source": "offline-fallback",
        "model": "local-template",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "employees": [
            {
                "firstName": f"AutoAI{_token()}",
                "middleName": f"M{_token(3)}",
                "lastName": f"EmpA{_token(4)}",
            },
            {
                "firstName": f"AutoAI{_token()}",
                "middleName": f"M{_token(3)}",
                "lastName": f"EmpB{_token(4)}",
            },
        ],
        "bookings": [
            {
                "firstname": f"Book{_token()}",
                "lastname": "Alpha",
                "totalprice": 150,
                "depositpaid": True,
                "bookingdates": {
                    "checkin": checkin1.isoformat(),
                    "checkout": checkout1.isoformat(),
                },
                "additionalneeds": "Breakfast",
            },
            {
                "firstname": f"Book{_token()}",
                "lastname": "Beta",
                "totalprice": 220,
                "depositpaid": False,
                "bookingdates": {
                    "checkin": checkin2.isoformat(),
                    "checkout": checkout2.isoformat(),
                },
                "additionalneeds": "LateCheckout",
            },
        ],
        "negativeApiScenarios": [
            {
                "name": "missing_firstname",
                "description": "Create booking without firstname",
                "expectedStatus": 400,
            },
            {
                "name": "unauthorized_update",
                "description": "PUT booking without token",
                "expectedStatus": 403,
            },
            {
                "name": "unauthorized_delete",
                "description": "DELETE booking without token",
                "expectedStatus": 403,
            },
            {
                "name": "missing_booking",
                "description": "GET non-existent booking id",
                "expectedStatus": 404,
            },
        ],
    }


def call_openai() -> tuple[str, dict]:
    from urllib import request

    api_key = os.environ["OPENAI_API_KEY"]
    model = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
    body = {
        "model": model,
        "messages": [
            {"role": "system", "content": "Return only JSON."},
            {"role": "user", "content": PROMPT},
        ],
        "temperature": 0.4,
    }
    req = request.Request(
        "https://api.openai.com/v1/chat/completions",
        data=json.dumps(body).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with request.urlopen(req, timeout=60) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    content = payload["choices"][0]["message"]["content"]
    content = content.strip()
    if content.startswith("```"):
        content = content.split("\n", 1)[1]
        content = content.rsplit("```", 1)[0]
    data = json.loads(content)
    data["source"] = "openai"
    data["model"] = model
    data["generatedAt"] = datetime.now(timezone.utc).isoformat()
    return content, data


def call_anthropic() -> tuple[str, dict]:
    from urllib import request

    api_key = os.environ["ANTHROPIC_API_KEY"]
    model = os.environ.get("ANTHROPIC_MODEL", "claude-3-5-haiku-latest")
    body = {
        "model": model,
        "max_tokens": 1200,
        "messages": [{"role": "user", "content": PROMPT}],
    }
    req = request.Request(
        "https://api.anthropic.com/v1/messages",
        data=json.dumps(body).encode("utf-8"),
        headers={
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    with request.urlopen(req, timeout=60) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    content = payload["content"][0]["text"]
    content = content.strip()
    if content.startswith("```"):
        content = content.split("\n", 1)[1]
        content = content.rsplit("```", 1)[0]
    data = json.loads(content)
    data["source"] = "anthropic"
    data["model"] = model
    data["generatedAt"] = datetime.now(timezone.utc).isoformat()
    return content, data


def validate_locally(data: dict) -> None:
    try:
        import jsonschema  # type: ignore
    except ImportError:
        # Minimal checks without jsonschema installed
        assert len(data.get("employees", [])) >= 2
        assert len(data.get("bookings", [])) >= 2
        assert len(data.get("negativeApiScenarios", [])) >= 3
        for b in data["bookings"]:
            assert b["bookingdates"]["checkout"] > b["bookingdates"]["checkin"]
            assert b["totalprice"] >= 0
        return

    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    jsonschema.validate(instance=data, schema=schema)
    for b in data["bookings"]:
        if b["bookingdates"]["checkout"] <= b["bookingdates"]["checkin"]:
            raise ValueError("checkout must be after checkin")
        if b["totalprice"] < 0:
            raise ValueError("totalprice must be >= 0")


def load_dotenv_files() -> None:
    """Load KEY=VALUE pairs from project .env files into os.environ (no overwrite)."""
    candidates = [
        ROOT.parent / ".env",
        ROOT / ".env",
        Path.cwd() / ".env",
    ]
    for env_path in candidates:
        if not env_path.is_file():
            continue
        try:
            for raw in env_path.read_text(encoding="utf-8").splitlines():
                line = raw.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                if line.startswith("export "):
                    line = line[len("export ") :].strip()
                key, _, value = line.partition("=")
                key = key.strip()
                value = value.strip().strip("'").strip('"')
                if key and key not in os.environ:
                    os.environ[key] = value
            print(f"Loaded env file: {env_path}")
        except OSError as exc:
            print(f"Could not read {env_path}: {exc}")


def main() -> int:
    load_dotenv_files()
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    VALIDATED_DIR.mkdir(parents=True, exist_ok=True)

    raw_text = ""
    data: dict
    try:
        if os.environ.get("OPENAI_API_KEY"):
            print("Using OPENAI_API_KEY for generation")
            raw_text, data = call_openai()
        elif os.environ.get("ANTHROPIC_API_KEY"):
            print("Using ANTHROPIC_API_KEY for generation")
            raw_text, data = call_anthropic()
        else:
            data = offline_payload()
            raw_text = json.dumps(data, indent=2)
            print("No LLM API key found; using offline-fallback generator.")
            print("Tip: put OPENAI_API_KEY=... in project .env (gitignored) or export it before running.")
    except Exception as exc:  # noqa: BLE001
        print(f"LLM call failed ({exc}); using offline-fallback.")
        data = offline_payload()
        raw_text = json.dumps(data, indent=2)

    ts = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    raw_json = RAW_DIR / f"ai_response_{ts}.json"
    raw_txt = RAW_DIR / f"ai_response_{ts}.txt"
    prompt_file = RAW_DIR / f"ai_prompt_{ts}.txt"

    raw_json.write_text(json.dumps(data, indent=2), encoding="utf-8")
    raw_txt.write_text(raw_text if raw_text else json.dumps(data, indent=2), encoding="utf-8")
    prompt_file.write_text(PROMPT, encoding="utf-8")

    validate_locally(data)
    validated = VALIDATED_DIR / "validated_test_data.json"
    validated.write_text(json.dumps(data, indent=2), encoding="utf-8")

    print(f"Raw artifact: {raw_json}")
    print(f"Validated artifact: {validated}")
    print(f"Source: {data.get('source')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
