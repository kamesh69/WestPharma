import { useCallback, useEffect, useMemo, useRef, useState } from "react";

type Suite = {
  id: string;
  label: string;
  description: string;
  ready: boolean;
  hint: string;
  module: string;
};

type Health = {
  os: string;
  java: boolean;
  maven: boolean;
  python: boolean;
  chrome: boolean;
  appiumMobile4723: boolean;
  appiumMac2_4724: boolean;
  desktopReady: boolean;
  mobileReady: boolean;
  adbDevices: string[];
  framing: string;
  activeSuite: string | null;
};

type Report = {
  path: string;
  module: string;
  name: string;
  mtime: number;
};

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init);
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.detail || res.statusText);
  }
  return res.json() as Promise<T>;
}

export default function App() {
  const [health, setHealth] = useState<Health | null>(null);
  const [suites, setSuites] = useState<Suite[]>([]);
  const [reports, setReports] = useState<Report[]>([]);
  const [runId, setRunId] = useState<string | null>(null);
  const [activeSuite, setActiveSuite] = useState<string | null>(null);
  const [log, setLog] = useState<string>("");
  const [status, setStatus] = useState<string>("idle");
  const [viewer, setViewer] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const logRef = useRef<HTMLPreElement>(null);

  const refresh = useCallback(async () => {
    try {
      const [h, s, r] = await Promise.all([
        api<Health>("/api/health"),
        api<Suite[]>("/api/suites"),
        api<Report[]>("/api/reports"),
      ]);
      setHealth(h);
      setSuites(s);
      setReports(r);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Backend unreachable");
    }
  }, []);

  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 8000);
    return () => clearInterval(t);
  }, [refresh]);

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, [log]);

  const pills = useMemo(() => {
    if (!health) return [];
    return [
      { label: `OS ${health.os}`, ok: true },
      { label: "Java", ok: health.java },
      { label: "Maven", ok: health.maven },
      { label: "Chrome", ok: health.chrome },
      { label: "Appium :4723", ok: health.appiumMobile4723 },
      { label: "Mac2 :4724", ok: health.appiumMac2_4724 },
      {
        label: health.adbDevices.length
          ? `ADB ${health.adbDevices.length}`
          : "No emulator",
        ok: health.adbDevices.length > 0,
      },
    ];
  }, [health]);

  async function startSuite(id: string) {
    setError(null);
    setLog("");
    setStatus("starting");
    setActiveSuite(id);
    try {
      const res = await api<{ runId: string }>("/api/runs/" + id, { method: "POST" });
      setRunId(res.runId);
      setStatus("running");
      const es = new EventSource(`/api/runs/${res.runId}/stream`);
      es.onmessage = (ev) => {
        setLog((prev) => (prev ? prev + "\n" + ev.data : ev.data));
      };
      es.addEventListener("done", (ev) => {
        const data = (ev as MessageEvent).data as string;
        setStatus(data.startsWith("passed") ? "passed" : data.split(":")[0]);
        es.close();
        refresh();
      });
      es.onerror = () => {
        es.close();
        setStatus((s) => (s === "running" ? "failed" : s));
        refresh();
      };
    } catch (e) {
      setStatus("failed");
      setActiveSuite(null);
      setError(e instanceof Error ? e.message : "Failed to start");
    }
  }

  async function stopRun() {
    if (!runId) return;
    await api(`/api/runs/${runId}/stop`, { method: "POST" });
    setStatus("stopped");
    refresh();
  }

  function openReport(path: string) {
    setViewer(`/api/files?path=${encodeURIComponent(path)}`);
    document.getElementById("reports")?.scrollIntoView({ behavior: "smooth" });
  }

  function openLatestReport() {
    if (reports[0]) openReport(reports[0].path);
  }

  return (
    <div className="app">
      <header className="hero">
        <h1 className="brand">
          West<span>Pharma</span>
        </h1>
        <p>
          AI-SDET Console — launch Web, API, Desktop, Mobile, and AI suites, watch
          live execution, and open Extent evidence in one place.
        </p>
        <div className="cta-row">
          <button className="btn btn-primary" type="button" onClick={() => startSuite("api")}>
            Run API smoke
          </button>
          <button className="btn btn-ghost" type="button" onClick={openLatestReport}>
            Open latest report
          </button>
        </div>
        {health?.framing ? (
          <p style={{ fontSize: "0.9rem", marginTop: "0.25rem" }}>{health.framing}</p>
        ) : null}
      </header>

      <section className="section">
        <h2>Environment</h2>
        <p className="lede">Health checks refresh automatically while you demo.</p>
        <div className="health-strip">
          {pills.map((p) => (
            <span key={p.label} className={`pill ${p.ok ? "ok" : "bad"}`}>
              {p.label}
            </span>
          ))}
        </div>
        {error ? <p style={{ color: "var(--danger)" }}>{error}</p> : null}
      </section>

      <section className="section">
        <h2>Suites</h2>
        <p className="lede">One suite at a time. Web runs headed Chrome; Desktop uses Mac2 → osascript → visible fallback on this Mac.</p>
        <div className="suite-grid">
          {suites.map((s) => (
            <article key={s.id} className="suite">
              <h3>{s.label}</h3>
              <p>{s.description}</p>
              <div className="meta">{s.hint}</div>
              <div className="suite-actions">
                <button
                  className="btn btn-primary"
                  type="button"
                  disabled={!s.ready || status === "running" || status === "starting"}
                  onClick={() => startSuite(s.id)}
                >
                  Run
                </button>
                <button
                  className="btn btn-ghost"
                  type="button"
                  onClick={() => {
                    const latest = reports.find((r) =>
                      r.module === s.module || r.module.startsWith(s.module)
                    );
                    if (latest) openReport(latest.path);
                  }}
                >
                  Report
                </button>
              </div>
            </article>
          ))}
        </div>

        <div className="panel">
          <div className="panel-head">
            <strong>
              Live log {activeSuite ? `— ${activeSuite}` : ""} [{status}]
            </strong>
            <button
              className="btn btn-ghost"
              type="button"
              disabled={!runId || (status !== "running" && status !== "starting")}
              onClick={stopRun}
            >
              Stop
            </button>
          </div>
          <pre className="log" ref={logRef}>
            {log || "Run a suite to stream Maven / Python output here."}
          </pre>
        </div>
      </section>

      <section className="section" id="reports">
        <h2>Reports & evidence</h2>
        <p className="lede">Extent HTML under reports/, plus desktop Calc_Summary files.</p>
        <div className="report-list">
          {reports.slice(0, 12).map((r) => (
            <div className="report-row" key={r.path}>
              <div>
                <strong>{r.name}</strong>
                <div>
                  <span>
                    {r.module} · {new Date(r.mtime * 1000).toLocaleString()}
                  </span>
                </div>
              </div>
              <button className="btn btn-ghost" type="button" onClick={() => openReport(r.path)}>
                Open
              </button>
            </div>
          ))}
          {reports.length === 0 ? <p className="lede">No reports yet — run a suite first.</p> : null}
        </div>
        {viewer ? (
          <div className="viewer">
            <iframe title="Report viewer" src={viewer} />
          </div>
        ) : null}
      </section>
    </div>
  );
}
