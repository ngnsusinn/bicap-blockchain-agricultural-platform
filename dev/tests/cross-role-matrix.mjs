#!/usr/bin/env node
/**
 * BICAP cross-role HTTP matrix probe.
 *
 * Runs against a REAL running backend (default http://localhost:8080) and replays the
 * cross-actor authorization matrix with plain HTTP, complementing
 * `CrossRoleMatrixIntegrationTest` (MockMvc). It logs in as every seeded role and checks
 * each feature from the other roles' point of view: admin⇄farm, farm⇄retailer,
 * retail⇄farm, shipping⇄driver, same-role peers, wrong-portal login and guests.
 *
 * Usage:
 *   node dev/tests/cross-role-matrix.mjs
 *   BASE_URL=http://localhost:8080 node dev/tests/cross-role-matrix.mjs
 *
 * Exit code 0 = every probe matched, 1 = at least one mismatch.
 */

const BASE = (process.env.BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

const ACCOUNTS = {
  FARM_A:      ['farm/login',     'farm@bicap.com',        'Farmpassword@2026'],
  FARM_B:      ['farm/login',     'farm@bicap.vn',         'Farmpassword@2026'],
  RETAIL_A:    ['retailer/login', 'retailer@bicap.com',    'Retailpassword@2026'],
  RETAIL_B:    ['retailer/login', 'retail@bicap.com',      'Retailpassword@2026'],
  SHIPPING:    ['shipping/login', 'shipping_mgr@bicap.com','Shipping@2026'],
  DRIVER_A:    ['driver/login',   'driver@bicap.com',      'Driver@2026'],
  DRIVER_B:    ['driver/login',   'driver2@bicap.com',     'Driver@2026'],
  ADMIN:       ['admin/login',    'admin@bicap.com',       'Adminpassword@2026'],
  SUPERADMIN:  ['admin/login',    'superadmin@bicap.com',  'Superadmin@2026'],
  MODERATOR:   ['admin/login',    'moderator@bicap.com',   'Moderator@2026'],
};

const EMAIL = {};
for (const [key, [, id]] of Object.entries(ACCOUNTS)) EMAIL[key] = id;

const tokens = {};
let pass = 0, fail = 0;
const failures = [];

async function login(key) {
  const [path, identifier, password] = ACCOUNTS[key];
  for (let attempt = 0; attempt < 2; attempt++) {
    const res = await fetch(`${BASE}/api/auth/${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifier, password }),
    });
    if (res.status === 429 && attempt === 0) {
      // The limiter keys on the socket address (X-Forwarded-For is ignored), so a re-run
      // within the same minute must wait for the window to roll over.
      console.log('  … auth rate limit hit, waiting 61s for the window to reset');
      await new Promise((resolve) => setTimeout(resolve, 61000));
      continue;
    }
    const body = await res.json().catch(() => ({}));
    if (!res.ok || !body.accessToken) {
      throw new Error(`login ${key} failed: HTTP ${res.status} ${JSON.stringify(body)}`);
    }
    tokens[key] = body.accessToken;
    return;
  }
  throw new Error(`login ${key} failed after rate-limit retry`);
}

async function req(method, path, token, { xActor, body, xff } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (xActor) headers['X-Actor-Email'] = xActor;
  if (xff) headers['X-Forwarded-For'] = xff;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(`${BASE}${path}`, {
    method, headers, body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  return { status: res.status, text };
}

/**
 * expect: number  → exact status
 *         'ALLOW' → authorized (anything except 401/403)
 *         'DENY'  → refused (401 or 403)
 *         '4xx'   → any 4xx
 */
function check(label, expected, actual) {
  let ok;
  if (typeof expected === 'number') ok = actual === expected;
  else if (expected === 'ALLOW') ok = actual !== 401 && actual !== 403;
  else if (expected === 'DENY') ok = actual === 401 || actual === 403;
  else if (expected === '4xx') ok = actual >= 400 && actual < 500;
  else if (typeof expected === 'boolean') ok = actual === expected;
  else ok = false;
  if (ok) { pass++; console.log(`  ✓ ${label} [${actual}]`); }
  else { fail++; failures.push(`${label}: expected ${expected} got ${actual}`); console.log(`  ✗ ${label} [expected ${expected}, got ${actual}]`); }
}

async function probe(label, method, path, actorKey, expected, opts = {}) {
  const token = actorKey ? tokens[actorKey] : null;
  const xActor = opts.xActor !== undefined ? opts.xActor : (opts.admin ? EMAIL[actorKey] : undefined);
  const { status } = await req(method, path, token, { xActor, body: opts.body });
  check(label, expected, status);
  return status;
}

async function getJson(path, token) {
  const { status, text } = await req('GET', path, token);
  if (status >= 400) throw new Error(`GET ${path} → ${status} ${text}`);
  return JSON.parse(text);
}

function section(title) { console.log(`\n${title}`); }

async function main() {
  console.log(`BICAP cross-role HTTP matrix → ${BASE}\n`);
  for (const key of Object.keys(ACCOUNTS)) await login(key);
  console.log(`logged in as ${Object.keys(tokens).length} actors`);

  // discover seeded resources
  const farmA = (await getJson('/api/farms/my', tokens.FARM_A)).find(f => f.status === 'APPROVED');
  if (!farmA) throw new Error('FARM_A has no APPROVED farm — run the seeder');
  const farmAId = farmA.id;
  const seasonA = (await getJson(`/api/farms/${farmAId}/seasons`, tokens.FARM_A)).content?.[0]
    || (await getJson(`/api/farms/${farmAId}/seasons`, tokens.FARM_A))[0];
  const seasonAId = seasonA.id;
  const publicProduct = (await getJson('/api/public/products')).content[0];
  const productId = publicProduct.id;

  // ── 1. portal segregation ────────────────────────────────────────────────
  section('1. Portal login is role-scoped');
  for (const [key, [path, id, pw]] of Object.entries(ACCOUNTS)) {
    const res = await req('POST', `/api/auth/${path}`, null, { body: { identifier: id, password: pw } });
    check(`correct portal: ${key}`, 200, res.status);
  }
  for (const [wrongPath, key] of [['admin/login', 'FARM_A'], ['retailer/login', 'FARM_A'],
                                   ['farm/login', 'RETAIL_A'], ['driver/login', 'SHIPPING'],
                                   ['shipping/login', 'DRIVER_A']]) {
    const [, id, pw] = ACCOUNTS[key];
    const res = await req('POST', `/api/auth/${wrongPath}`, null, { body: { identifier: id, password: pw } });
    check(`${key} cannot log into /${wrongPath}`, 401, res.status);
  }

  // ── 2. header spoofing ───────────────────────────────────────────────────
  section('2. X-Actor-Email cannot impersonate another account');
  await probe('farm JWT + superadmin header → /api/admins', 'GET', '/api/admins', 'FARM_A', 'DENY', { xActor: EMAIL.SUPERADMIN });
  await probe('retail JWT + admin header → /api/admin/farms', 'GET', '/api/admin/farms', 'RETAIL_A', 'DENY', { xActor: EMAIL.ADMIN });
  await probe('admin JWT + own header → /api/admins', 'GET', '/api/admins', 'ADMIN', 200, { xActor: EMAIL.ADMIN });
  await probe('admin JWT, no header → /api/admins', 'GET', '/api/admins', 'ADMIN', 400, { xActor: null });

  // ── 3. farm self-service ─────────────────────────────────────────────────
  section('3. Farm self-service (farm ⇄ farm, farm ⇄ others)');
  await probe('owner reads own farm', 'GET', `/api/farms/${farmAId}`, 'FARM_A', 200);
  await probe('peer farm reads it', 'GET', `/api/farms/${farmAId}`, 'FARM_B', 'DENY');
  await probe('retailer reads it', 'GET', `/api/farms/${farmAId}`, 'RETAIL_A', 'DENY');
  await probe('driver reads it', 'GET', `/api/farms/${farmAId}`, 'DRIVER_A', 'DENY');
  await probe('admin reads it (farm-owned route)', 'GET', `/api/farms/${farmAId}`, 'ADMIN', 'DENY');
  await probe('anonymous reads it', 'GET', `/api/farms/${farmAId}`, null, 'DENY');
  await probe('retailer registers a farm', 'POST', '/api/farms/register', 'RETAIL_A',
    'DENY', { body: { name: `probe-${Date.now()}`, address: 'x', area: 1, gpsLat: 10, gpsLng: 106 } });

  // ── 4. farm approval ─────────────────────────────────────────────────────
  section('4. Farm approval (admin-view vs admin-write)');
  await probe('admin lists farms', 'GET', '/api/admin/farms', 'ADMIN', 200, { admin: true });
  await probe('moderator lists farms', 'GET', '/api/admin/farms', 'MODERATOR', 200, { admin: true });
  await probe('farm lists approval queue', 'GET', '/api/admin/farms', 'FARM_A', 'DENY', { admin: true });
  await probe('anonymous lists approval queue', 'GET', '/api/admin/farms', null, 'DENY');

  // ── 5. seasons / processes / exports ─────────────────────────────────────
  section('5. Season / export tenant isolation');
  await probe('owner lists seasons', 'GET', `/api/farms/${farmAId}/seasons`, 'FARM_A', 200);
  await probe('peer lists seasons', 'GET', `/api/farms/${farmAId}/seasons`, 'FARM_B', 'DENY');
  await probe('retailer lists seasons', 'GET', `/api/farms/${farmAId}/seasons`, 'RETAIL_A', 'DENY');
  await probe('owner legacy exports', 'GET', `/api/seasons/${seasonAId}/exports`, 'FARM_A', 200);
  await probe('peer legacy exports', 'GET', `/api/seasons/${seasonAId}/exports`, 'FARM_B', 'DENY');
  await probe('moderator legacy exports (admin-view)', 'GET', `/api/seasons/${seasonAId}/exports`, 'MODERATOR', 200);
  await probe('anonymous unknown trace', 'GET', '/api/trace/0xdeadbeef', null, 404);

  // ── 6. product monitoring ────────────────────────────────────────────────
  section('6. Product monitoring (admin ⇄ retail/farm)');
  await probe('admin lists products', 'GET', '/api/admin/products', 'ADMIN', 200, { admin: true });
  await probe('moderator lists products', 'GET', '/api/admin/products', 'MODERATOR', 200, { admin: true });
  await probe('retailer with own header', 'GET', '/api/admin/products', 'RETAIL_A', 'DENY', { xActor: EMAIL.RETAIL_A });
  await probe('retailer WITHOUT header (must not downgrade)', 'GET', '/api/admin/products', 'RETAIL_A', 'DENY', { xActor: null });
  await probe('driver WITHOUT header (must not downgrade)', 'GET', '/api/admin/products', 'DRIVER_A', 'DENY', { xActor: null });
  await probe('moderator changes product status', 'PUT', `/api/admin/products/${productId}/status`, 'MODERATOR', 'DENY',
    { admin: true, body: { status: 'INACTIVE' } });
  await probe('anonymous admin products', 'GET', '/api/admin/products', null, 'DENY');

  // ── 7. marketplace ───────────────────────────────────────────────────────
  section('7. Marketplace (retailer-only vs public)');
  await probe('retailer searches marketplace', 'GET', '/api/marketplace/products', 'RETAIL_A', 200);
  await probe('farm searches marketplace', 'GET', '/api/marketplace/products', 'FARM_A', 'DENY');
  await probe('anonymous marketplace search', 'GET', '/api/marketplace/products', null, 'DENY');
  await probe('anonymous public catalogue', 'GET', '/api/public/products', null, 200);
  await probe('anonymous education', 'GET', '/api/public/education', null, 200);
  await probe('anonymous categories', 'GET', '/api/categories', null, 200);

  // ── 8. orders ────────────────────────────────────────────────────────────
  section('8. Orders (farm ⇄ retailer both directions)');
  await probe('farm lists orders', 'GET', '/api/orders', 'FARM_A', 200);
  await probe('retailer uses farm order list', 'GET', '/api/orders', 'RETAIL_A', 'DENY');
  await probe('retailer lists own orders', 'GET', '/api/orders/my', 'RETAIL_A', 200);
  await probe('farm uses retailer order list', 'GET', '/api/orders/my', 'FARM_A', 'DENY');
  await probe('anonymous order list', 'GET', '/api/orders', null, 'DENY');

  // ── 9. shipping ⇄ driver ─────────────────────────────────────────────────
  section('9. Shipping ⇄ driver portal segregation');
  for (const path of ['/api/shipping/orders/ready-to-ship', '/api/shipping/shipments',
                      '/api/shipping/vehicles', '/api/shipping/drivers']) {
    await probe(`shipping reads ${path}`, 'GET', path, 'SHIPPING', 200);
    await probe(`farm reads ${path}`, 'GET', path, 'FARM_A', 'DENY');
    await probe(`driver reads ${path}`, 'GET', path, 'DRIVER_A', 'DENY');
  }
  await probe('driver lists own shipments', 'GET', '/api/driver/shipments', 'DRIVER_B', 200);
  await probe('farm uses driver app', 'GET', '/api/driver/shipments', 'FARM_A', 'DENY');
  await probe('shipping uses driver app', 'GET', '/api/driver/shipments', 'SHIPPING', 'DENY');
  await probe('anonymous driver app', 'GET', '/api/driver/shipments', null, 'DENY');
  await probe('farm creates vehicle', 'POST', '/api/shipping/vehicles', 'FARM_A', 'DENY',
    { body: { licensePlate: `probe-${Date.now()}`, type: 'Xe', capacity: 10 } });

  // ── 10. IoT ──────────────────────────────────────────────────────────────
  section('10. IoT readings (owner ⇄ peer ⇄ admin override)');
  const reading = { farmId: farmAId, temperature: 26.5, humidity: 65, ph: 6.4 };
  await probe('owner pushes reading', 'POST', '/api/iot/sensors', 'FARM_A', 200, { body: reading });
  await probe('peer pushes reading', 'POST', '/api/iot/sensors', 'FARM_B', 'DENY', { body: reading });
  await probe('retailer pushes reading', 'POST', '/api/iot/sensors', 'RETAIL_A', 'DENY', { body: reading });
  await probe('admin pushes reading (documented bypass)', 'POST', '/api/iot/sensors', 'ADMIN', 200, { body: reading });
  await probe('out-of-range reading', 'POST', '/api/iot/sensors', 'FARM_A', 400,
    { body: { farmId: farmAId, temperature: 99, humidity: 1, ph: 14 } });

  // ── 11. reports ──────────────────────────────────────────────────────────
  section('11. Reports (any role submits, only admin handles)');
  const report = { type: 'INCIDENT', subject: `probe-${Date.now()}`, content: 'Noi dung bao cao probe tren muoi ky tu.' };
  for (const key of ['FARM_A', 'RETAIL_A', 'DRIVER_A', 'SHIPPING', 'ADMIN']) {
    const res = await req('POST', '/api/reports', tokens[key], { body: report });
    check(`${key} submits report`, 201, res.status);
    if (res.status === 201) {
      const id = JSON.parse(res.text).id;
      await probe(`moderator cannot handle report`, 'PUT', `/api/reports/admin/${id}/handle`, 'MODERATOR', 'DENY',
        { body: { status: 'RESOLVED', adminResponse: 'probe' } });
      await probe(`admin handles report`, 'PUT', `/api/reports/admin/${id}/handle`, 'ADMIN', 200,
        { body: { status: 'RESOLVED', adminResponse: 'probe' } });
    }
  }
  await probe('farm reads admin reports', 'GET', '/api/reports/admin', 'FARM_A', 'DENY');
  await probe('moderator reads admin reports', 'GET', '/api/reports/admin', 'MODERATOR', 200);

  // ── 12. notifications ────────────────────────────────────────────────────
  section('12. Notifications (guest ⇄ user, broadcast role)');
  await probe('anonymous system feed', 'GET', '/api/notifications', null, 200);
  await probe('anonymous unread count', 'GET', '/api/notifications/unread-count', null, 'DENY');
  await probe('farm unread count', 'GET', '/api/notifications/unread-count', 'FARM_A', 200);
  const broadcast = { target: 'FARM_MANAGER', title: `probe-${Date.now()}`, content: 'Noi dung broadcast probe tren muoi ky tu.', sendEmail: false };
  await probe('farm broadcasts', 'POST', '/api/notifications/broadcast', 'FARM_A', 'DENY', { body: broadcast });
  await probe('retailer broadcasts', 'POST', '/api/notifications/broadcast', 'RETAIL_A', 'DENY', { body: broadcast });
  await probe('shipping broadcasts', 'POST', '/api/notifications/broadcast', 'SHIPPING', 201, { body: broadcast });

  // ── 13. packages & subscriptions ─────────────────────────────────────────
  section('13. Service packages & subscriptions');
  await probe('anonymous package list', 'GET', '/api/service-packages', null, 200);
  await probe('admin all packages', 'GET', '/api/service-packages/admin/all', 'ADMIN', 200);
  await probe('moderator all packages', 'GET', '/api/service-packages/admin/all', 'MODERATOR', 200);
  await probe('farm all packages', 'GET', '/api/service-packages/admin/all', 'FARM_A', 'DENY');
  await probe('anonymous all packages', 'GET', '/api/service-packages/admin/all', null, 'DENY');
  await probe('moderator creates package', 'POST', '/api/service-packages/admin', 'MODERATOR', 'DENY',
    { body: { name: `probe-${Date.now()}`, description: 'x', price: '1000', durationDays: '30' } });
  await probe('owner farm subscriptions', 'GET', `/api/subscriptions/farm/${farmAId}`, 'FARM_A', 200);
  await probe('peer farm subscriptions', 'GET', `/api/subscriptions/farm/${farmAId}`, 'FARM_B', 'DENY');
  await probe('admin farm subscriptions', 'GET', `/api/subscriptions/farm/${farmAId}`, 'ADMIN', 200);

  // ── 14. blockchain & contracts ───────────────────────────────────────────
  section('14. Blockchain & smart contracts (admin ladder)');
  await probe('admin ledger', 'GET', '/api/blockchain/transactions', 'ADMIN', 200, { admin: true });
  await probe('moderator ledger', 'GET', '/api/blockchain/transactions', 'MODERATOR', 200, { admin: true });
  await probe('farm ledger', 'GET', '/api/blockchain/transactions', 'FARM_A', 'DENY', { admin: true });
  await probe('anonymous ledger', 'GET', '/api/blockchain/transactions', null, 'DENY');
  await probe('moderator retries tx', 'POST', '/api/blockchain/transactions/999999/retry', 'MODERATOR', 'DENY', { admin: true });
  await probe('moderator reads contracts', 'GET', '/api/admin/contracts', 'MODERATOR', 200, { admin: true });
  await probe('farm reads contracts', 'GET', '/api/admin/contracts', 'FARM_A', 'DENY', { admin: true });

  // ── 15. admin accounts ───────────────────────────────────────────────────
  section('15. Admin account management (superadmin-only writes)');
  await probe('superadmin lists admins', 'GET', '/api/admins', 'SUPERADMIN', 200, { admin: true });
  await probe('moderator lists admins', 'GET', '/api/admins', 'MODERATOR', 200, { admin: true });
  await probe('farm lists admins', 'GET', '/api/admins', 'FARM_A', 'DENY', { admin: true });
  await probe('anonymous lists admins', 'GET', '/api/admins', null, 'DENY');
  await probe('admin creates admin', 'POST', '/api/admins', 'ADMIN', 'DENY',
    { admin: true, body: { fullName: 'probe', email: `probe.${Date.now()}@bicap.com`, password: 'CrossRole@2026', role: 'ADMIN' } });

  // ── 16. rate limiting (CR-06) ────────────────────────────────────────────
  section('16. Rate limiting ignores client-supplied X-Forwarded-For');
  let limited = 0;
  for (let i = 0; i < 40 && limited === 0; i++) {
    const res = await req('POST', '/api/auth/login', null,
      { body: { identifier: 'probe.nobody@bicap.com', password: 'x' }, xff: `1.2.3.${i}` });
    if (res.status === 429) limited++;
  }
  check(`rotating X-Forwarded-For still hits the 30/min limit (${limited} x 429)`, true, limited > 0);

  // ── summary ──────────────────────────────────────────────────────────────
  console.log(`\n${'─'.repeat(60)}`);
  console.log(`RESULT: ${pass} passed, ${fail} failed`);
  if (failures.length) {
    console.log('\nFailures:');
    for (const f of failures) console.log(`  - ${f}`);
  }
  process.exit(fail === 0 ? 0 : 1);
}

main().catch((e) => { console.error(`FATAL: ${e.message}`); process.exit(1); });
