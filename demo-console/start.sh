#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONSOLE="$(cd "$(dirname "$0")" && pwd)"
BACKEND="$CONSOLE/backend"
FRONTEND="$CONSOLE/frontend"
VENV="$BACKEND/.venv"
API_PORT=8765
UI_PORT=5173

export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"

pick_python() {
  for c in python3.13 python3.12 python3.11 python3; do
    if command -v "$c" >/dev/null 2>&1; then
      ver="$("$c" -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')"
      major="${ver%%.*}"
      minor="${ver#*.}"
      if [[ "$major" -eq 3 && "$minor" -ge 11 && "$minor" -le 13 ]]; then
        echo "$c"
        return 0
      fi
    fi
  done
  echo "Need Python 3.11–3.13 for the demo console (found incompatible default)." >&2
  exit 1
}

PY="$(pick_python)"

echo "==> WestPharma Demo Console"
echo "    project: $ROOT"
echo "    python:  $PY ($("$PY" --version 2>&1))"

if [[ ! -x "$VENV/bin/python" ]]; then
  "$PY" -m venv "$VENV"
fi
# Recreate if venv python is 3.14+
VENV_MINOR="$("$VENV/bin/python" -c 'import sys; print(sys.version_info.minor)' 2>/dev/null || echo 99)"
if [[ "$VENV_MINOR" -gt 13 ]]; then
  echo "Recreating venv (was Python 3.$VENV_MINOR; need <=3.13)"
  "$PY" -m venv --clear "$VENV"
fi

# shellcheck disable=SC1091
source "$VENV/bin/activate"
pip install -q -r "$BACKEND/requirements.txt"

if [[ ! -d "$FRONTEND/node_modules" ]]; then
  (cd "$FRONTEND" && npm install)
fi

cleanup() {
  echo "Shutting down..."
  [[ -n "${API_PID:-}" ]] && kill "$API_PID" 2>/dev/null || true
  [[ -n "${UI_PID:-}" ]] && kill "$UI_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "==> API on http://127.0.0.1:${API_PORT}"
(cd "$BACKEND" && uvicorn main:app --host 127.0.0.1 --port "$API_PORT") &
API_PID=$!

echo "==> UI on http://127.0.0.1:${UI_PORT}"
(cd "$FRONTEND" && npm run dev -- --host 127.0.0.1 --port "$UI_PORT") &
UI_PID=$!

sleep 2
if command -v open >/dev/null 2>&1; then
  open "http://127.0.0.1:${UI_PORT}" || true
fi

echo "Console ready. Ctrl+C to stop."
wait
