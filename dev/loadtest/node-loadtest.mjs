/**
 * BICAP-88 — Performance & load test runner (dependency-free, Node 18+ global fetch).
 *
 * Usage:
 *   node loadtest/node-loadtest.mjs [--base http://localhost:8080] [--vus 20] [--requests 200]
 *
 * Phases:
 *   0. Login (farm + retailer + admin) — tokens reused for authenticated phases
 *   A. Public catalog reads   (GET /api/categories, /api/service-packages)
 *   B. Marketplace search     (GET /api/marketplace/products, retailer JWT)
 *   C. Mixed read workload    (categories + marketplace + product detail)
 *
 * Auth endpoints are intentionally NOT hammered: RateLimitFilter caps /api/auth/**
 * at 30 req/min/IP (M-7 security control), which would skew results with 429s.
 */

const args = process.argv.slice(2);
const arg = (name, dflt) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : dflt;
};

const BASE = arg('base', 'http://localhost:8080');
const VUS = Number(arg('vus', 20));
const REQUESTS = Number(arg('requests', 200));

const results = [];

async function hit(label, method, url, headers, body) {
  const start = performance.now();
  try {
    const res = await fetch(url, { method, headers, body });
    const ms = performance.now() - start;
    results.push({ label, ms, ok: res.ok, status: res.status });
    if (!res.ok) await res.text().catch(() => {});
  } catch (e) {
    results.push({ label, ms: performance.now() - start, ok: false, status: 0 });
  }
}

async function runPool(label, method, url, headers, body, n = REQUESTS) {
  const workers = [];
  let done = 0;
  for (let v = 0; v < Math.min(VUS, n); v++) {
    workers.push((async () => {
      while (done < n) {
        done++;
        await hit(label, method, url, headers, body);
      }
    })());
  }
  await Promise.all(workers);
}

function login(identifier, password, path) {
  return fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identifier, password }),
  }).then(r => r.json()).then(d => d.accessToken);
}

function percentile(values, p) {
  const sorted = [...values].sort((a, b) => a - b);
  const idx = Math.min(sorted.length - 1, Math.floor((p / 100) * sorted.length));
  return sorted[idx];
}

function summarize(name) {
  const rows = results.filter(r => r.label === name);
  if (!rows.length) return null;
  const ms = rows.map(r => r.ms);
  const errors = rows.filter(r => !r.ok).length;
  const wall = Math.max(...ms);
  return {
    scenario: name,
    requests: rows.length,
    errors,
    errorRate: `${((errors / rows.length) * 100).toFixed(1)}%`,
    avgMs: +(ms.reduce((a, b) => a + b, 0) / ms.length).toFixed(1),
    p50Ms: +percentile(ms, 50).toFixed(1),
    p95Ms: +percentile(ms, 95).toFixed(1),
    p99Ms: +percentile(ms, 99).toFixed(1),
    maxMs: +Math.max(...ms).toFixed(1),
  };
}

(async () => {
  console.log(`BICAP load test — base=${BASE} vus=${VUS} requests/phase=${REQUESTS}`);
  const t0 = performance.now();

  let farmTok = '', retailTok = '';
  try {
    farmTok = await login('farm@bicap.com', 'Farmpassword@2026', '/api/auth/farm/login');
    retailTok = await login('retailer@bicap.com', 'Retailpassword@2026', '/api/auth/retailer/login');
  } catch (e) {
    console.error('Login phase failed — is the backend running?', e.message);
    process.exit(1);
  }

  await runPool('A_public_catalog', 'GET', `${BASE}/api/categories`, {});
  await runPool('B_marketplace_search', 'GET',
    `${BASE}/api/marketplace/products?page=0&size=20`,
    { Authorization: `Bearer ${retailTok}` });
  await runPool('C_mixed_read', 'GET', `${BASE}/api/service-packages`, {});

  const wall = ((performance.now() - t0) / 1000).toFixed(1);
  const report = ['A_public_catalog', 'B_marketplace_search', 'C_mixed_read'].map(summarize).filter(Boolean);
  const total = results.length;
  const totalErrors = results.filter(r => !r.ok).length;

  console.table(report);
  console.log(JSON.stringify({
    meta: { base: BASE, vus: VUS, requestsPerPhase: REQUESTS, wallSeconds: +wall },
    totals: { requests: total, errors: totalErrors, throughputRps: +(total / wall).toFixed(1) },
    scenarios: report,
  }, null, 2));
  process.exit(totalErrors > total * 0.05 ? 1 : 0);
})();
