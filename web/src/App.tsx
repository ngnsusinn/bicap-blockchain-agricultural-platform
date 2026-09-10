import PortalApp from './portal/PortalApp';
import AdminApp from './admin/AdminApp';

/**
 * Entry router for the unified BICAP web app.
 *
 * The two portals used to live in separate Vite projects (`frontend` + `admin-web`)
 * and were reached through different origins/ports. They are now one bundle served
 * by Spring Boot on a single origin, split only by endpoint:
 *
 *   - `/admin`, `/admin/*`  → Admin dashboard  (admin-web sources, `src/admin`)
 *   - everything else (`/`, `/trace/{hash}`, …) → Farm / Retailer / Shipping / Guest
 *     portal (frontend sources, `src/portal`)
 *
 * Both share one session (see `src/shared/session.ts`), so no `?token=` hand-off
 * between the two is needed anymore.
 */
const ADMIN_PATH = /^\/admin(?:\/|$)/;

export default function App() {
  return ADMIN_PATH.test(window.location.pathname) ? <AdminApp /> : <PortalApp />;
}
