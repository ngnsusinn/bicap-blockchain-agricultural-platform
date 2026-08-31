// BICAP-88 — k6 load test (https://k6.io). Run:
//   k6 run loadtest/k6-loadtest.js -e BASE_URL=http://localhost:8080
//
// Scenarios:
//   smoke        : 1 VU, 30s   — sanity
//   load         : 50 VUs, 2m  — expected production traffic
//   stress       : 200 VUs, 2m — find breaking point
// Thresholds: p95 < 800ms for reads, error rate < 1%.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const errors = new Counter('bicap_errors');

export const options = {
  scenarios: {
    smoke:  { executor: 'constant-vus', vus: 1,   duration: '30s',  tags: { scenario: 'smoke' } },
    load:   { executor: 'ramping-vus',  startVUs: 0,
              stages: [{ duration: '30s', target: 50 }, { duration: '2m', target: 50 }, { duration: '30s', target: 0 }],
              tags: { scenario: 'load' }, startTime: '40s' },
  },
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    http_req_failed: ['rate<0.01'],
    bicap_errors: ['count<50'],
  },
};

const PUBLIC_PARAMS = { headers: { 'Content-Type': 'application/json' } };

export function setup() {
  const res = http.post(`${BASE}/api/auth/retailer/login`,
    JSON.stringify({ identifier: 'retailer@bicap.com', password: 'Retailpassword@2026' }), PUBLIC_PARAMS);
  check(res, { 'retailer login 200': r => r.status === 200 }) || errors.add(1);
  let token = '';
  try { token = res.json('accessToken'); } catch (e) { /* keep empty */ }
  return { token };
}

export default function (data) {
  // Marketplace browsing (public read path, cached by Redis/in-memory layer)
  const search = http.get(`${BASE}/api/marketplace/products?page=0&size=20`,
    { headers: { Authorization: `Bearer ${data.token}` } });
  if (!check(search, { 'search 200': r => r.status === 200 })) errors.add(1);

  // Category catalog (cache-heavy endpoint)
  const cats = http.get(`${BASE}/api/categories`);
  if (!check(cats, { 'categories 200': r => r.status === 200 })) errors.add(1);

  // Service packages (public)
  const pkgs = http.get(`${BASE}/api/service-packages`);
  if (!check(pkgs, { 'packages 200': r => r.status === 200 })) errors.add(1);

  sleep(1);
}
