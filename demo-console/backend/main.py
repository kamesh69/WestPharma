"""WestPharma AI-SDET Demo Console — localhost orchestrator."""

from __future__ import annotations

import asyncio
import os
import platform
import shutil
import signal
import subprocess
import time
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.parse import unquote
from urllib.request import urlopen

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, StreamingResponse
from pydantic import BaseModel

ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / "reports"
AI_VALIDATED = ROOT / "ai-helper" / "artifacts" / "validated"
AI_SCRIPT = ROOT / "ai-helper" / "generate_test_data.py"

SUITES: dict[str, dict[str, Any]] = {
    "web": {
        "id": "web",
        "label": "Q1 Web",
        "module": "web",
        "description": "OrangeHRM employee lifecycle (Chrome headed)",
        "kind": "maven",
        "args": ["-pl", "web-automation", "-am", "test", "-Dweb.headless=false"],
    },
    "api": {
        "id": "api",
        "label": "Q2 API",
        "module": "api",
        "description": "Restful-Booker CRUD + negatives",
        "kind": "maven",
        "args": ["-pl", "api-automation", "-am", "test"],
    },
    "desktop": {
        "id": "desktop",
        "label": "Q3 Desktop",
        "module": "desktop",
        "description": "Calculator → summary → TextEdit/Notepad (Mac2 / WinAppDriver)",
        "kind": "maven",
        "args": ["-pl", "desktop-automation", "-am", "test"],
    },
    "mobile": {
        "id": "mobile",
        "label": "Q4 Mobile",
        "module": "mobile",
        "description": "Android Calculator via Appium (visible emulator)",
        "kind": "maven",
        "args": ["-pl", "mobile-automation", "-am", "test"],
    },
    "ai": {
        "id": "ai",
        "label": "Q5 AI Helper",
        "module": "ai",
        "description": "Generate + validate OpenAI test data",
        "kind": "python",
        "args": [str(AI_SCRIPT)],
    },
}

runs: dict[str, dict[str, Any]] = {}
active_suite: str | None = None
_lock = asyncio.Lock()

app = FastAPI(title="WestPharma AI-SDET Demo Console", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://127.0.0.1:5173",
        "http://localhost:5173",
        "http://127.0.0.1:4173",
        "http://localhost:4173",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class RunResponse(BaseModel):
    runId: str
    suite: str
    status: str


def _http_ok(url: str, timeout: float = 1.5) -> bool:
    try:
        with urlopen(url, timeout=timeout) as resp:
            return 200 <= getattr(resp, "status", 200) < 500
    except Exception:
        return False


def _which(cmd: str) -> str | None:
    return shutil.which(cmd)


def _run_capture(cmd: list[str], timeout: float = 8.0) -> tuple[int, str]:
    try:
        p = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=timeout,
            cwd=str(ROOT),
        )
        out = (p.stdout or "") + (p.stderr or "")
        return p.returncode, out.strip()
    except Exception as ex:
        return 1, str(ex)


def build_health() -> dict[str, Any]:
    os_name = platform.system()
    java_ok = _which("java") is not None
    mvn_ok = _which("mvn") is not None
    python_ok = _which("python3") is not None or _which("python") is not None
    chrome_ok = (
        Path("/Applications/Google Chrome.app").exists()
        or _which("google-chrome") is not None
        or _which("chrome") is not None
    )
    adb = _which("adb")
    devices: list[str] = []
    if adb:
        code, out = _run_capture([adb, "devices"])
        if code == 0:
            for line in out.splitlines()[1:]:
                if "\tdevice" in line:
                    devices.append(line.split("\t")[0])
    appium_mobile = _http_ok("http://127.0.0.1:4723/status")
    appium_mac2 = _http_ok("http://127.0.0.1:4724/status")
    winapp = _http_ok("http://127.0.0.1:4723/status") if os_name == "Windows" else False

    # macOS: Mac2 preferred; osascript/System Events always available as fallback
    desktop_ready = (os_name == "Darwin") or (os_name == "Windows")
    mobile_ready = appium_mobile and len(devices) > 0

    return {
        "os": os_name,
        "projectRoot": str(ROOT),
        "java": java_ok,
        "maven": mvn_ok,
        "python": python_ok,
        "chrome": chrome_ok,
        "adbDevices": devices,
        "appiumMobile4723": appium_mobile,
        "appiumMac2_4724": appium_mac2,
        "winAppDriver": winapp,
        "desktopReady": desktop_ready,
        "mobileReady": mobile_ready,
        "framing": "Same use case; WinAppDriver on Windows, Appium Mac2 on macOS.",
        "activeSuite": active_suite,
        "checkedAt": datetime.now(timezone.utc).isoformat(),
    }


def list_reports() -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    if not REPORTS.exists():
        return items
    for path in REPORTS.rglob("ExtentReport_*.html"):
        rel = path.relative_to(ROOT).as_posix()
        module = path.parent.name
        if path.parent.name == "sample":
            module = path.parent.parent.name + "/sample"
        items.append(
            {
                "path": rel,
                "module": module,
                "name": path.name,
                "mtime": path.stat().st_mtime,
                "size": path.stat().st_size,
            }
        )
    for path in (ROOT / "desktop-automation" / "output").glob("Calc_Summary_*.txt"):
        rel = path.relative_to(ROOT).as_posix()
        items.append(
            {
                "path": rel,
                "module": "desktop-output",
                "name": path.name,
                "mtime": path.stat().st_mtime,
                "size": path.stat().st_size,
            }
        )
    items.sort(key=lambda x: x["mtime"], reverse=True)
    return items


def list_ai_artifacts() -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    if not AI_VALIDATED.exists():
        return items
    for path in AI_VALIDATED.glob("*.json"):
        items.append(
            {
                "path": path.relative_to(ROOT).as_posix(),
                "name": path.name,
                "mtime": path.stat().st_mtime,
            }
        )
    items.sort(key=lambda x: x["mtime"], reverse=True)
    return items


def _safe_path(rel: str) -> Path:
    raw = unquote(rel).lstrip("/")
    candidate = (ROOT / raw).resolve()
    if not str(candidate).startswith(str(ROOT.resolve())):
        raise HTTPException(status_code=400, detail="Path escapes project root")
    allowed_roots = [
        REPORTS.resolve(),
        (ROOT / "desktop-automation" / "output").resolve(),
        AI_VALIDATED.resolve(),
        (ROOT / "ai-helper" / "artifacts").resolve(),
    ]
    if not any(str(candidate).startswith(str(r)) for r in allowed_roots):
        raise HTTPException(status_code=403, detail="File type not allowed")
    if not candidate.is_file():
        raise HTTPException(status_code=404, detail="File not found")
    return candidate


def _build_command(suite: str) -> list[str]:
    meta = SUITES[suite]
    env_path = os.environ.get("PATH", "")
    if meta["kind"] == "maven":
        mvn = _which("mvn")
        if not mvn:
            raise HTTPException(status_code=500, detail="mvn not found on PATH")
        return [mvn, *meta["args"]]
    py = _which("python3") or _which("python")
    if not py:
        raise HTTPException(status_code=500, detail="python3 not found on PATH")
    return [py, *meta["args"]]


async def _pump_process(run_id: str, cmd: list[str], suite: str) -> None:
    global active_suite
    run = runs[run_id]
    run["status"] = "running"
    run["startedAt"] = time.time()
    env = os.environ.copy()
    env["PROJECT_ROOT"] = str(ROOT)
    # Ensure headed web for demos
    if suite == "web":
        env["MAVEN_OPTS"] = (env.get("MAVEN_OPTS") or "") + ""
    try:
        proc = await asyncio.create_subprocess_exec(
            *cmd,
            cwd=str(ROOT),
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.STDOUT,
            env=env,
            start_new_session=True,
        )
        run["pid"] = proc.pid
        assert proc.stdout is not None
        while True:
            line = await proc.stdout.readline()
            if not line:
                break
            text = line.decode(errors="replace")
            run["log"].append(text.rstrip("\n"))
            if len(run["log"]) > 4000:
                run["log"] = run["log"][-3000:]
        code = await proc.wait()
        run["exitCode"] = code
        run["status"] = "passed" if code == 0 else "failed"
    except Exception as ex:
        run["log"].append(f"[console] error: {ex}")
        run["status"] = "failed"
        run["exitCode"] = -1
    finally:
        run["finishedAt"] = time.time()
        active_suite = None


@app.get("/api/health")
def health() -> dict[str, Any]:
    return build_health()


@app.get("/api/suites")
def suites() -> list[dict[str, Any]]:
    health = build_health()
    out = []
    for s in SUITES.values():
        ready = True
        hint = "Ready"
        if s["id"] == "desktop":
            ready = bool(health["desktopReady"])
            if health["os"] == "Darwin":
                hint = (
                    "Mac2 :4724 ready"
                    if health["appiumMac2_4724"]
                    else "Ready (osascript fallback; optional Mac2 on :4724)"
                )
            elif health["os"] == "Windows":
                hint = "WinAppDriver required on Windows"
            else:
                hint = "Desktop Q3 needs macOS or Windows"
                ready = False
        elif s["id"] == "mobile":
            ready = bool(health["mobileReady"])
            hint = "Need emulator device + Appium :4723" if not ready else "Emulator + Appium OK"
        elif s["id"] == "web":
            ready = health["chrome"] and health["maven"]
            hint = "Chrome + Maven required" if not ready else "Ready (headed Chrome)"
        elif s["id"] == "api":
            ready = health["maven"]
            hint = "Maven required" if not ready else "Ready"
        elif s["id"] == "ai":
            ready = health["python"]
            hint = "python3 required" if not ready else "Ready (uses OPENAI_API_KEY if set)"
        out.append({**s, "ready": ready, "hint": hint})
    return out


@app.post("/api/runs/{suite}", response_model=RunResponse)
async def start_run(suite: str) -> RunResponse:
    global active_suite
    if suite not in SUITES:
        raise HTTPException(status_code=404, detail="Unknown suite")
    async with _lock:
        if active_suite is not None:
            raise HTTPException(
                status_code=409,
                detail=f"Suite '{active_suite}' is already running",
            )
        cmd = _build_command(suite)
        run_id = uuid.uuid4().hex[:12]
        runs[run_id] = {
            "id": run_id,
            "suite": suite,
            "status": "starting",
            "log": [f"[console] $ {' '.join(cmd)}", f"[console] cwd={ROOT}"],
            "exitCode": None,
            "pid": None,
            "startedAt": None,
            "finishedAt": None,
        }
        active_suite = suite
        asyncio.create_task(_pump_process(run_id, cmd, suite))
        return RunResponse(runId=run_id, suite=suite, status="starting")


@app.get("/api/runs/{run_id}")
def get_run(run_id: str) -> dict[str, Any]:
    run = runs.get(run_id)
    if not run:
        raise HTTPException(status_code=404, detail="Run not found")
    return run


@app.get("/api/runs/{run_id}/stream")
async def stream_run(run_id: str) -> StreamingResponse:
    if run_id not in runs:
        raise HTTPException(status_code=404, detail="Run not found")

    async def event_gen():
        idx = 0
        while True:
            run = runs[run_id]
            log = run["log"]
            while idx < len(log):
                line = log[idx].replace("\r", "")
                yield f"data: {line}\n\n"
                idx += 1
            if run["status"] in ("passed", "failed", "stopped"):
                yield f"event: done\ndata: {run['status']}:{run.get('exitCode')}\n\n"
                break
            await asyncio.sleep(0.35)

    return StreamingResponse(event_gen(), media_type="text/event-stream")


@app.post("/api/runs/{run_id}/stop")
async def stop_run(run_id: str) -> dict[str, str]:
    global active_suite
    run = runs.get(run_id)
    if not run:
        raise HTTPException(status_code=404, detail="Run not found")
    pid = run.get("pid")
    if pid and run["status"] in ("starting", "running"):
        try:
            os.killpg(pid, signal.SIGTERM)
        except Exception:
            try:
                os.kill(pid, signal.SIGTERM)
            except Exception:
                pass
        run["status"] = "stopped"
        run["log"].append("[console] stopped by user")
        run["finishedAt"] = time.time()
        active_suite = None
    return {"status": run["status"]}


@app.get("/api/reports")
def reports() -> list[dict[str, Any]]:
    return list_reports()


@app.get("/api/artifacts/ai")
def ai_artifacts() -> list[dict[str, Any]]:
    return list_ai_artifacts()


@app.get("/api/files")
def get_file(path: str = Query(..., description="Path relative to project root")) -> FileResponse:
    file_path = _safe_path(path)
    media = "text/html" if file_path.suffix == ".html" else "text/plain"
    if file_path.suffix == ".json":
        media = "application/json"
    return FileResponse(file_path, media_type=media, filename=file_path.name)


@app.get("/api/runs")
def list_runs() -> list[dict[str, Any]]:
    return sorted(runs.values(), key=lambda r: r.get("startedAt") or 0, reverse=True)[:20]
