package vn.courses.ut.edu.javaprogramming.bicap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * BICAP cross-role authorization matrix — every feature is exercised from every actor's
 * point of view: admin ⇄ farm, farm ⇄ retailer, retailer ⇄ farm, shipping ⇄ driver,
 * same-role peer (tenant isolation), wrong-portal login, and guest/anonymous.
 *
 * <p>Unlike the per-service unit tests, this suite boots the full security filter chain and
 * real H2 seeder, so it also covers the intersections that only appear at HTTP level:
 * <ul>
 *   <li>JWT principal vs. the legacy {@code X-Actor-Email} admin header (spoof attempts)</li>
 *   <li>role check present/absent while ownership still holds</li>
 *   <li>one actor of a role reading or mutating another actor's resource (IDOR)</li>
 *   <li>an actor using the wrong portal's endpoint family</li>
 * </ul>
 *
 * <p>Fixtures are created through the public API by the owning actor (farm self-register →
 * admin approve → subscription → season → harvest → export → listing → admin approve →
 * retailer order → farm accept → shipping shipment), i.e. the fixture setup itself is a
 * cross-role test. Fixtures are namespaced with a random suffix so the shared in-memory
 * database of the Maven run stays deterministic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrossRoleMatrixIntegrationTest {

    private static final String FARM_A_EMAIL = "farm@bicap.com";
    private static final String FARM_A_PW = "Farmpassword@2026";
    private static final String FARM_B_EMAIL = "farm@bicap.vn";
    private static final String RETAIL_A_EMAIL = "retailer@bicap.com";
    private static final String RETAIL_A_PW = "Retailpassword@2026";
    private static final String RETAIL_B_EMAIL = "retail@bicap.com";
    private static final String SHIPPING_EMAIL = "shipping_mgr@bicap.com";
    private static final String SHIPPING_PW = "Shipping@2026";
    private static final String DRIVER_A_EMAIL = "driver@bicap.com";
    private static final String DRIVER_B_EMAIL = "driver2@bicap.com";
    private static final String DRIVER_PW = "Driver@2026";
    private static final String ADMIN_EMAIL = "admin@bicap.com";
    private static final String ADMIN_PW = "Adminpassword@2026";
    private static final String SUPERADMIN_EMAIL = "superadmin@bicap.com";
    private static final String SUPERADMIN_PW = "Superadmin@2026";
    private static final String MODERATOR_EMAIL = "moderator@bicap.com";
    private static final String MODERATOR_PW = "Moderator@2026";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;
    @Autowired FarmRepository farmRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired DriverRepository driverRepository;
    @Autowired NotificationRepository notificationRepository;

    private final Map<String, String> tokens = new ConcurrentHashMap<>();
    private final Map<String, String> actorEmail = new LinkedHashMap<>();
    private final String run = Long.toString(System.nanoTime() % 1_000_000_00L);

    // ── fixtures ──────────────────────────────────────────────────────────────
    private long farmX;            // APPROVED, owned by FARM_A, active subscription
    private long farmY;            // APPROVED, owned by FARM_B, active subscription
    private long seasonX;
    private long seasonY;
    private long exportX;
    private long exportY;
    private long productX;         // ACTIVE, backed by exportX
    private long categoryId;
    private long orderPendingX;    // RETAIL_B PENDING on productX  (farm list/detail matrix)
    private long orderAcceptX;     // RETAIL_B PENDING            (accept success path)
    private long orderRejectX;     // RETAIL_B PENDING            (reject success path)
    private long orderCancelX;     // RETAIL_B PENDING            (cancel success path)
    private long orderDepositX;    // RETAIL_A ACCEPTED           (deposit matrix)
    private long orderShipX;       // RETAIL_A DEPOSIT_PAID       (shipment + driver E2E)
    private long shipmentX;        // PICKING_UP, assigned to DRIVER_B
    private long pendingFarmX;     // PENDING, owned by FARM_A    (approval matrix)
    private String traceX;

    // ── boot ──────────────────────────────────────────────────────────────────

    @BeforeAll
    void bootstrap() throws Exception {
        // One distinct socket address per actor: the rate limiter keys on the socket address
        // (it ignores the client-supplied X-Forwarded-For), so sharing 127.0.0.1 across every
        // login in the run could trip the 30/min auth limit.
        tokens.put("FARM_A", login("farm/login", FARM_A_EMAIL, FARM_A_PW, "cr-farm-a"));
        tokens.put("FARM_B", login("farm/login", FARM_B_EMAIL, FARM_A_PW, "cr-farm-b"));
        tokens.put("RETAIL_A", login("retailer/login", RETAIL_A_EMAIL, RETAIL_A_PW, "cr-retail-a"));
        tokens.put("RETAIL_B", login("retailer/login", RETAIL_B_EMAIL, RETAIL_A_PW, "cr-retail-b"));
        tokens.put("SHIPPING", login("shipping/login", SHIPPING_EMAIL, SHIPPING_PW, "cr-ship"));
        tokens.put("DRIVER_A", login("driver/login", DRIVER_A_EMAIL, DRIVER_PW, "cr-driver-a"));
        tokens.put("DRIVER_B", login("driver/login", DRIVER_B_EMAIL, DRIVER_PW, "cr-driver-b"));
        tokens.put("ADMIN", login("admin/login", ADMIN_EMAIL, ADMIN_PW, "cr-admin"));
        tokens.put("SUPERADMIN", login("admin/login", SUPERADMIN_EMAIL, SUPERADMIN_PW, "cr-superadmin"));
        tokens.put("MODERATOR", login("admin/login", MODERATOR_EMAIL, MODERATOR_PW, "cr-moderator"));

        actorEmail.put("FARM_A", FARM_A_EMAIL);
        actorEmail.put("FARM_B", FARM_B_EMAIL);
        actorEmail.put("RETAIL_A", RETAIL_A_EMAIL);
        actorEmail.put("RETAIL_B", RETAIL_B_EMAIL);
        actorEmail.put("SHIPPING", SHIPPING_EMAIL);
        actorEmail.put("DRIVER_A", DRIVER_A_EMAIL);
        actorEmail.put("DRIVER_B", DRIVER_B_EMAIL);
        actorEmail.put("ADMIN", ADMIN_EMAIL);
        actorEmail.put("SUPERADMIN", SUPERADMIN_EMAIL);
        actorEmail.put("MODERATOR", MODERATOR_EMAIL);

        categoryId = read(perform(get("/api/categories"), null)).get(0).path("id").asLong();

        farmX = createApprovedFarm("FARM_A", "CRX " + run, 10.9, 106.8);
        farmY = createApprovedFarm("FARM_B", "CRY " + run, 11.8, 108.3);
        activateSubscription(farmX, "FARM_A");
        activateSubscription(farmY, "FARM_B");

        seasonX = createHarvestedSeason(farmX, "FARM_A", "Vu CRX " + run);
        seasonY = createHarvestedSeason(farmY, "FARM_B", "Vu CRY " + run);

        exportX = createExport(farmX, seasonX, "FARM_A", "Kho X " + run);
        exportY = createExport(farmY, seasonY, "FARM_B", "Kho Y " + run);
        traceX = read(perform(get("/api/trace/" + traceHashOf(exportX)), null)).path("traceHash").asText();

        productX = registerListing(farmX, exportX, "FARM_A", "Cai xanh CRX " + run);
        approveProduct(productX);

        orderPendingX = placeOrder(productX, "RETAIL_B", 4);
        orderAcceptX = placeOrder(productX, "RETAIL_B", 2);
        orderRejectX = placeOrder(productX, "RETAIL_B", 2);
        orderCancelX = placeOrder(productX, "RETAIL_B", 2);
        orderDepositX = placeOrder(productX, "RETAIL_A", 3);
        orderShipX = placeOrder(productX, "RETAIL_A", 2);
        acceptOrder(orderDepositX, "FARM_A");
        acceptOrder(orderShipX, "FARM_A");

        Order shippingOrder = orderRepository.findById(orderShipX).orElseThrow();
        shippingOrder.setStatus(Order.STATUS_DEPOSIT_PAID);
        orderRepository.save(shippingOrder);

        long vehicleId = createVehicle("SHIPPING", "CR-" + run);
        long driverBId = driverRepository.findByUserId(
                userRepository.findByEmail(DRIVER_B_EMAIL).orElseThrow().getId()).orElseThrow().getId();
        shipmentX = createShipment(orderShipX, driverBId, vehicleId, "SHIPPING", "CR route");

        pendingFarmX = registerFarm("FARM_A", "CRP " + run, 12.0, 106.0);
        // two extra PENDING farms so approve/reject success paths have fresh targets
        registerFarm("FARM_A", "CRP2 " + run, 12.5, 106.1);
    }

    private String traceHashOf(long exportId) throws Exception {
        // exportX was captured from the fixture creation; look it up through the owner listing.
        JsonNode list = read(perform(get("/api/farms/" + farmX + "/exports"), tokens.get("FARM_A")));
        for (JsonNode node : list) {
            if (node.path("id").asLong() == exportId) {
                return node.path("traceHash").asText();
            }
        }
        throw new IllegalStateException("export " + exportId + " not found in farm listing");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String login(String path, String identifier, String password, String clientIp) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/" + path)
                        .with(req -> { req.setRemoteAddr(clientIp); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("identifier", identifier, "password", password))))
                .andReturn();
        JsonNode body = read(r);
        String token = body.path("accessToken").asText();
        assertFalse(token.isBlank(), () -> "login failed for " + identifier + " via " + path + ": " + safeBody(r));
        return token;
    }

    private MvcResult perform(MockHttpServletRequestBuilder req, String token) throws Exception {
        if (token != null && !token.isBlank()) {
            req.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(req).andReturn();
    }

    private MvcResult performAdmin(MockHttpServletRequestBuilder req, String token, String email) throws Exception {
        req.header("X-Actor-Email", email);
        return perform(req, token);
    }

    private MvcResult postJson(String url, Object body, String tokenOrActor) throws Exception {
        // Accept either a raw JWT or an actor key ("FARM_A", "ADMIN", ...) for readability.
        String token = tokens.getOrDefault(tokenOrActor, tokenOrActor);
        return perform(post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body)), token);
    }

    private void expectStatus(int expected, MockHttpServletRequestBuilder req, String token, String label) throws Exception {
        MvcResult r = perform(req, token);
        int actual = r.getResponse().getStatus();
        assertEquals(expected, actual, () -> label + " → expected " + expected + " got " + actual + " body=" + safeBody(r));
    }

    private void expectAdminStatus(int expected, MockHttpServletRequestBuilder req, String token, String email, String label) throws Exception {
        MvcResult r = performAdmin(req, token, email);
        int actual = r.getResponse().getStatus();
        assertEquals(expected, actual, () -> label + " → expected " + expected + " got " + actual + " body=" + safeBody(r));
    }

    /** Overload for requests already executed through {@link #postJson}. */
    private void expectStatus(int expected, MvcResult r, String label) {
        int actual = r.getResponse().getStatus();
        assertEquals(expected, actual, () -> label + " → expected " + expected + " got " + actual + " body=" + safeBody(r));
    }

    /** Overload for requests already executed through {@link #postJson}. */
    private void expectDenied(MvcResult r, String label) {
        int actual = r.getResponse().getStatus();
        assertTrue(actual == 401 || actual == 403,
                () -> label + " → expected 401/403 got " + actual + " body=" + safeBody(r));
    }

    /** Asserts the request is refused on identity/role grounds (401 unauthenticated or 403 forbidden). */
    private void expectDenied(MockHttpServletRequestBuilder req, String token, String label) throws Exception {
        MvcResult r = perform(req, token);
        int actual = r.getResponse().getStatus();
        assertTrue(actual == 401 || actual == 403,
                () -> label + " → expected 401/403 got " + actual + " body=" + safeBody(r));
    }

    private void expectAdminDenied(MockHttpServletRequestBuilder req, String token, String email, String label) throws Exception {
        MvcResult r = performAdmin(req, token, email);
        int actual = r.getResponse().getStatus();
        assertTrue(actual == 401 || actual == 403,
                () -> label + " → expected 401/403 got " + actual + " body=" + safeBody(r));
    }

    private JsonNode read(MvcResult r) throws Exception {
        String body = r.getResponse().getContentAsString();
        return body == null || body.isBlank() ? om.createObjectNode() : om.readTree(body);
    }

    private String toJson(Object o) throws Exception {
        return om.writeValueAsString(o);
    }

    private static String safeBody(MvcResult r) {
        try {
            return r.getResponse().getContentAsString();
        } catch (Exception e) {
            return "<unreadable>";
        }
    }

    // ── fixture builders (all through the API, as the owning actor) ───────────

    private long registerFarm(String ownerKey, String name, double lat, double lng) throws Exception {
        MvcResult r = postJson("/api/farms/register", Map.of(
                "name", name, "address", "Dia chi " + name, "area", 10.0,
                "gpsLat", lat, "gpsLng", lng, "description", "Trang trai kiem thu cross-role",
                "productTypes", "Rau an la"), tokens.get(ownerKey));
        assertEquals(201, r.getResponse().getStatus(), () -> "farm register " + name + " body=" + safeBody(r));
        return read(r).path("id").asLong();
    }

    private long createApprovedFarm(String ownerKey, String name, double lat, double lng) throws Exception {
        long id = registerFarm(ownerKey, name, lat, lng);
        expectAdminStatus(200, put("/api/admin/farms/" + id + "/approve"), tokens.get("ADMIN"), ADMIN_EMAIL,
                "admin approves farm " + id);
        return id;
    }

    private void activateSubscription(long farmId, String ownerKey) throws Exception {
        long packageId = read(perform(get("/api/service-packages"), null)).get(0).path("id").asLong();
        MvcResult r = postJson("/api/subscriptions/purchase",
                Map.of("packageId", packageId, "farmId", farmId), tokens.get(ownerKey));
        assertTrue(r.getResponse().getStatus() < 300,
                () -> "purchase package for farm " + farmId + " body=" + safeBody(r));
        subscriptionRepository.findByFarmIdAndStatus(farmId, SubscriptionStatus.PENDING_PAYMENT)
                .ifPresent(sub -> {
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    subscriptionRepository.save(sub);
                });
    }

    private long createHarvestedSeason(long farmId, String ownerKey, String name) throws Exception {
        MvcResult r = postJson("/api/farms/" + farmId + "/seasons", Map.of(
                "name", name, "productType", "Rau an la", "variety", "Cai xanh",
                "area", 5.0, "startDate", LocalDate.now().minusDays(30).toString()), tokens.get(ownerKey));
        assertEquals(201, r.getResponse().getStatus(), () -> "season create body=" + safeBody(r));
        long seasonId = read(r).path("id").asLong();

        MvcResult harvest = perform(patch("/api/farms/" + farmId + "/seasons/" + seasonId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("status", "HARVESTED", "harvestedQuantity", 500, "harvestUnit", "kg"))),
                tokens.get(ownerKey));
        assertEquals(200, harvest.getResponse().getStatus(), () -> "harvest body=" + safeBody(harvest));
        return seasonId;
    }

    private long createExport(long farmId, long seasonId, String ownerKey, String warehouse) throws Exception {
        MvcResult r = perform(post("/api/farms/" + farmId + "/seasons/" + seasonId + "/export")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", "cr-export-" + UUID.randomUUID())
                .content(toJson(Map.of("quantity", 200, "unit", "kg",
                        "exportDate", LocalDate.now().toString(), "warehouse", warehouse))), tokens.get(ownerKey));
        assertTrue(r.getResponse().getStatus() < 300, () -> "export create body=" + safeBody(r));
        assertEquals("READY", read(r).path("status").asText(), () -> "export not READY: " + safeBody(r));
        return read(r).path("id").asLong();
    }

    private long registerListing(long farmId, long exportId, String ownerKey, String name) throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile request = new MockMultipartFile("request", "", "application/json",
                om.writeValueAsBytes(Map.of(
                        "exportId", exportId, "name", name,
                        "description", "Mo ta san pham kiem thu cross-role day du tren nam muoi ky tu.",
                        "quantity", 100, "price", 15000, "categoryId", categoryId)));
        MvcResult r = perform(multipart("/api/farms/" + farmId + "/marketplace/products")
                .file(request).file(image), tokens.get(ownerKey));
        assertEquals(201, r.getResponse().getStatus(), () -> "listing body=" + safeBody(r));
        return read(r).path("id").asLong();
    }

    private void approveProduct(long productId) throws Exception {
        expectAdminStatus(200, put("/api/admin/products/" + productId + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"),
                tokens.get("ADMIN"), ADMIN_EMAIL, "admin approves product " + productId);
    }

    private long placeOrder(long productId, String retailerKey, double qty) throws Exception {
        MvcResult r = postJson("/api/orders", Map.of(
                "productId", productId, "quantity", qty, "deliveryAddr", "12 Tran Duy Hung, Ha Noi",
                "proposedPrice", 15000, "desiredDeliveryDate", LocalDate.now().plusDays(5).toString(),
                "notes", "don cross-role"), tokens.get(retailerKey));
        assertEquals(200, r.getResponse().getStatus(), () -> "place order body=" + safeBody(r));
        return read(r).path("id").asLong();
    }

    private void acceptOrder(long orderId, String farmKey) throws Exception {
        expectStatus(200, put("/api/orders/" + orderId + "/accept"), tokens.get(farmKey), "accept order " + orderId);
    }

    private long createVehicle(String shippingKey, String plate) throws Exception {
        MvcResult r = postJson("/api/shipping/vehicles",
                Map.of("licensePlate", plate, "type", "Xe tai", "capacity", 500.0), tokens.get(shippingKey));
        assertEquals(201, r.getResponse().getStatus(), () -> "vehicle body=" + safeBody(r));
        return read(r).path("id").asLong();
    }

    private long createShipment(long orderId, long driverId, long vehicleId, String shippingKey, String route) throws Exception {
        MvcResult r = postJson("/api/shipping/shipments",
                Map.of("orderId", orderId, "driverId", driverId, "vehicleId", vehicleId, "routeSummary", route),
                tokens.get(shippingKey));
        assertEquals(201, r.getResponse().getStatus(), () -> "shipment body=" + safeBody(r));
        return read(r).path("id").asLong();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AUTH & PORTAL SEGREGATION
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void portalLogin_isRoleScoped_noCrossPortalUpgrade() throws Exception {
        // Correct portal → 200
        expectStatus(200, post("/api/auth/farm/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", FARM_A_EMAIL, "password", FARM_A_PW))), null, "farm→farm");
        expectStatus(200, post("/api/auth/retailer/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", RETAIL_A_EMAIL, "password", RETAIL_A_PW))), null, "retail→retail");
        expectStatus(200, post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", MODERATOR_EMAIL, "password", MODERATOR_PW))), null, "moderator→admin");
        expectStatus(200, post("/api/auth/shipping/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", SHIPPING_EMAIL, "password", SHIPPING_PW))), null, "shipping→shipping");
        expectStatus(200, post("/api/auth/driver/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", DRIVER_A_EMAIL, "password", DRIVER_PW))), null, "driver→driver");

        // Wrong portal with correct credentials → 401 (no silent role upgrade, M-3)
        expectDenied(post("/api/auth/farm/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", RETAIL_A_EMAIL, "password", RETAIL_A_PW))), null, "retail→farm");
        expectDenied(post("/api/auth/retailer/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", FARM_A_EMAIL, "password", FARM_A_PW))), null, "farm→retail");
        expectDenied(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", FARM_A_EMAIL, "password", FARM_A_PW))), null, "farm→admin");
        expectDenied(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", RETAIL_A_EMAIL, "password", RETAIL_A_PW))), null, "retail→admin");
        expectDenied(post("/api/auth/shipping/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", DRIVER_A_EMAIL, "password", DRIVER_PW))), null, "driver→shipping");
        expectDenied(post("/api/auth/driver/login").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("identifier", SHIPPING_EMAIL, "password", SHIPPING_PW))), null, "shipping→driver");
    }

    @Test
    void adminHeader_cannotBeSpoofed_byAnotherAuthenticatedActor() throws Exception {
        // A farm JWT plus an admin's X-Actor-Email must NOT authenticate: the header must
        // match the JWT subject, otherwise the filter leaves the request unauthenticated.
        expectDenied(get("/api/admins").header("X-Actor-Email", SUPERADMIN_EMAIL),
                tokens.get("FARM_A"), "farm JWT + superadmin header");
        expectDenied(get("/api/admin/farms").header("X-Actor-Email", ADMIN_EMAIL),
                tokens.get("RETAIL_A"), "retail JWT + admin header");
        expectDenied(post("/api/admin/announcements").header("X-Actor-Email", ADMIN_EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("type", "INFO", "title", "spoof", "content", "noi dung gia mao"))),
                tokens.get("DRIVER_A"), "driver JWT + admin header");

        // Admin JWT with their own header → allowed (control)
        expectAdminStatus(200, get("/api/admins"), tokens.get("ADMIN"), ADMIN_EMAIL, "admin→admins");
    }

    @Test
    void adminEndpoints_withoutRequiredHeader_areClientErrors_evenForAdminJwt() throws Exception {
        // X-Actor-Email is a required header on the admin controllers → 400 when absent,
        // not a silent success. This is the shape the guest-regression suite relies on.
        expectStatus(400, get("/api/admins"), tokens.get("ADMIN"), "admin JWT, no header");
        expectStatus(400, get("/api/admin/farms"), tokens.get("ADMIN"), "admin JWT, no header");
        expectStatus(400, get("/api/admin/dashboard"), tokens.get("ADMIN"), "admin JWT, no header");
        expectStatus(400, get("/api/admin/contracts"), tokens.get("ADMIN"), "admin JWT, no header");
        expectStatus(400, get("/api/blockchain/transactions"), tokens.get("ADMIN"), "admin JWT, no header");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FARM SELF-SERVICE (BICAP-9) — admin ⇄ farm, farm ⇄ farm, farm ⇄ retailer
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void farmSelfService_roleAndTenantMatrix() throws Exception {
        // owner reads own farm
        expectStatus(200, get("/api/farms/" + farmX), tokens.get("FARM_A"), "owner reads own farm");
        // peer farm / other roles cannot read it
        expectDenied(get("/api/farms/" + farmX), tokens.get("FARM_B"), "peer farm reads farm");
        expectDenied(get("/api/farms/" + farmX), tokens.get("RETAIL_A"), "retailer reads farm");
        expectDenied(get("/api/farms/" + farmX), tokens.get("DRIVER_A"), "driver reads farm");
        expectDenied(get("/api/farms/" + farmX), tokens.get("SHIPPING"), "shipping reads farm");
        expectDenied(get("/api/farms/" + farmX), tokens.get("ADMIN"), "admin on farm self-service (role-gated)");
        expectDenied(get("/api/farms/" + farmX), null, "anonymous reads farm");

        // my-farms is scoped to the caller's own farms
        JsonNode mineA = read(perform(get("/api/farms/my"), tokens.get("FARM_A")));
        List<Long> idsA = new ArrayList<>();
        mineA.forEach(f -> idsA.add(f.path("id").asLong()));
        assertTrue(idsA.contains(farmX), "FARM_A must see its own farm");
        assertFalse(idsA.contains(farmY), "FARM_A must not see FARM_B's farm");
        JsonNode mineB = read(perform(get("/api/farms/my"), tokens.get("FARM_B")));
        List<Long> idsB = new ArrayList<>();
        mineB.forEach(f -> idsB.add(f.path("id").asLong()));
        assertFalse(idsB.contains(farmX), "FARM_B must not see FARM_A's farm");

        // update: owner ok, peer + other roles denied
        Map<String, Object> update = Map.of("name", "CRX " + run, "address", "Dia chi moi",
                "area", 11.0, "gpsLat", 10.9, "gpsLng", 106.8, "description", "cap nhat", "productTypes", "Rau");
        expectStatus(200, put("/api/farms/" + farmX).contentType(MediaType.APPLICATION_JSON).content(toJson(update)),
                tokens.get("FARM_A"), "owner updates farm");
        expectDenied(put("/api/farms/" + farmX).contentType(MediaType.APPLICATION_JSON).content(toJson(update)),
                tokens.get("FARM_B"), "peer updates farm");
        expectDenied(put("/api/farms/" + farmX).contentType(MediaType.APPLICATION_JSON).content(toJson(update)),
                tokens.get("RETAIL_A"), "retailer updates farm");

        // certifications listing follows ownership
        expectStatus(200, get("/api/farms/" + farmX + "/certifications"), tokens.get("FARM_A"), "owner lists certs");
        expectDenied(get("/api/farms/" + farmX + "/certifications"), tokens.get("FARM_B"), "peer lists certs");

        // registerFarm is a farm-manager action: another role must not create a farm profile
        expectDenied(postJson("/api/farms/register", Map.of(
                        "name", "Retail farm " + run, "address", "addr", "area", 1.0,
                        "gpsLat", 10.0, "gpsLng", 106.0, "description", "x", "productTypes", "y"),
                "RETAIL_A"), "retailer registers a farm");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FARM APPROVAL (BICAP-3/4) — farm cannot approve itself, moderator read-only
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void farmApproval_adminViewVsAdminWrite_andFarmCannotSelfApprove() throws Exception {
        expectAdminStatus(200, get("/api/admin/farms"), tokens.get("ADMIN"), ADMIN_EMAIL, "admin lists farms");
        expectAdminStatus(200, get("/api/admin/farms"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator lists farms");
        expectAdminStatus(200, get("/api/admin/farms/stats"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator stats");
        expectAdminStatus(200, get("/api/admin/farms/" + pendingFarmX), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator farm detail");
        expectAdminDenied(get("/api/admin/farms"), tokens.get("FARM_A"), FARM_A_EMAIL, "farm reads approval queue");
        expectAdminDenied(get("/api/admin/farms"), tokens.get("RETAIL_A"), RETAIL_A_EMAIL, "retailer reads approval queue");
        expectDenied(get("/api/admin/farms"), null, "anonymous reads approval queue");

        // the owner cannot approve their own farm, and a moderator cannot write
        expectAdminDenied(put("/api/admin/farms/" + pendingFarmX + "/approve"), tokens.get("FARM_A"), FARM_A_EMAIL,
                "farm self-approves");
        expectAdminDenied(put("/api/admin/farms/" + pendingFarmX + "/approve"), tokens.get("MODERATOR"), MODERATOR_EMAIL,
                "moderator approves (write)");
        expectAdminDenied(put("/api/admin/farms/" + pendingFarmX + "/reject").contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("action", "REJECT", "reason", "ho so thieu"))),
                tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator rejects (write)");
        expectAdminStatus(200, put("/api/admin/farms/" + pendingFarmX + "/approve"), tokens.get("ADMIN"), ADMIN_EMAIL,
                "admin approves pending farm");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SEASON / PROCESS / EXPORT — farm ⇄ farm, farm ⇄ retailer, admin view
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void seasonAndProcess_tenantIsolation() throws Exception {
        expectStatus(200, get("/api/farms/" + farmX + "/seasons"), tokens.get("FARM_A"), "owner lists seasons");
        expectDenied(get("/api/farms/" + farmX + "/seasons"), tokens.get("FARM_B"), "peer lists seasons");
        expectDenied(get("/api/farms/" + farmX + "/seasons"), tokens.get("RETAIL_A"), "retailer lists seasons");
        expectDenied(get("/api/farms/" + farmX + "/seasons"), tokens.get("ADMIN"), "admin on season self-service");
        expectDenied(get("/api/farms/" + farmX + "/seasons/" + seasonX), tokens.get("FARM_B"), "peer season detail");

        // creating a season under someone else's farm is refused for every non-owner
        Map<String, Object> seasonBody = Map.of("name", "Vu la " + run, "productType", "Rau",
                "variety", "Cai", "area", 2.0, "startDate", LocalDate.now().minusDays(5).toString());
        expectDenied(postJson("/api/farms/" + farmX + "/seasons", seasonBody, "FARM_B"), "peer creates season");
        expectDenied(postJson("/api/farms/" + farmX + "/seasons", seasonBody, "RETAIL_A"), "retailer creates season");

        // process endpoints follow the same tenant boundary
        expectStatus(200, get("/api/seasons/" + seasonX + "/processes"), tokens.get("FARM_A"), "owner lists processes");
        expectDenied(get("/api/seasons/" + seasonX + "/processes"), tokens.get("FARM_B"), "peer lists processes");

        // an in-progress season is required for a new process; this one is HARVESTED → 400 for owner
        expectStatus(400, postJson("/api/seasons/" + seasonX + "/processes", Map.of(
                        "processType", "SEEDING", "executionDate", LocalDate.now().minusDays(10).toString(),
                        "materials", "hat", "notes", "gieo"), "FARM_A"), "process on harvested season");
    }

    @Test
    void seasonExport_tenantIsolation_andAdminReadOnly() throws Exception {
        // owner sees own exports, peer cannot, admin-view may read
        expectStatus(200, get("/api/seasons/" + seasonX + "/exports"), tokens.get("FARM_A"), "owner reads legacy exports");
        expectDenied(get("/api/seasons/" + seasonX + "/exports"), tokens.get("FARM_B"), "peer reads legacy exports");
        expectStatus(200, get("/api/seasons/" + seasonX + "/exports"), tokens.get("MODERATOR"), "moderator reads legacy exports");
        expectStatus(200, get("/api/seasons/" + seasonX + "/exports"), tokens.get("ADMIN"), "admin reads legacy exports");
        // The legacy /api/seasons/{id}/exports/{exportId} endpoint reads the legacy Export
        // table, not SeasonExport; the access decision is what matters here.
        expectStatus(404, get("/api/seasons/" + seasonX + "/exports/999999"), tokens.get("FARM_A"), "owner legacy export detail");
        expectDenied(get("/api/seasons/" + seasonX + "/exports/999999"), tokens.get("FARM_B"), "peer legacy export detail");

        // new-style export endpoint: role + ownership
        expectStatus(200, get("/api/farms/" + farmX + "/exports"), tokens.get("FARM_A"), "owner reads farm exports");
        expectDenied(get("/api/farms/" + farmX + "/exports"), tokens.get("FARM_B"), "peer reads farm exports");
        expectDenied(get("/api/farms/" + farmX + "/exports"), tokens.get("RETAIL_A"), "retailer reads farm exports");

        // a non-owner cannot export for someone else's season
        expectDenied(post("/api/farms/" + farmX + "/seasons/" + seasonX + "/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "cr-peer-" + UUID.randomUUID())
                        .content(toJson(Map.of("quantity", 1, "unit", "kg",
                                "exportDate", LocalDate.now().toString(), "warehouse", "Kho la"))),
                tokens.get("FARM_B"), "peer creates export");
        expectDenied(post("/api/farms/" + farmX + "/seasons/" + seasonX + "/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "cr-retail-" + UUID.randomUUID())
                        .content(toJson(Map.of("quantity", 1, "unit", "kg",
                                "exportDate", LocalDate.now().toString(), "warehouse", "Kho la"))),
                tokens.get("RETAIL_A"), "retailer creates export");

        // public trace is anonymous and returns the real trace hash
        expectStatus(200, get("/api/trace/" + traceX), null, "anonymous trace");
        expectStatus(404, get("/api/trace/0xdeadbeef"), null, "anonymous trace unknown hash");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TRADING FLOOR + PRODUCT MONITORING — farm ⇄ admin, retail ⇄ admin
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void tradingFloor_isFarmOwned_andAdminApprovalGatesRetailerVisibility() throws Exception {
        expectStatus(200, get("/api/farms/" + farmX + "/marketplace/products"), tokens.get("FARM_A"), "owner lists listings");
        expectDenied(get("/api/farms/" + farmX + "/marketplace/products"), tokens.get("FARM_B"), "peer lists listings");
        expectDenied(get("/api/farms/" + farmX + "/marketplace/products"), tokens.get("RETAIL_A"), "retailer lists farm listings");

        // registering a product for a farm the actor does not own is refused
        MockMultipartFile image = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile request = new MockMultipartFile("request", "", "application/json",
                om.writeValueAsBytes(Map.of("exportId", exportX, "name", "Hang la " + run,
                        "description", "Mo ta san pham kiem thu cross-role day du tren nam muoi ky tu.",
                        "quantity", 1, "price", 1000, "categoryId", categoryId)));
        MvcResult peer = perform(multipart("/api/farms/" + farmX + "/marketplace/products")
                .file(request).file(image), tokens.get("FARM_B"));
        assertTrue(peer.getResponse().getStatus() == 401 || peer.getResponse().getStatus() == 403,
                () -> "peer registers listing → expected 401/403 got " + peer.getResponse().getStatus()
                        + " body=" + safeBody(peer));

        // public category catalogue is anonymous
        expectStatus(200, get("/api/categories"), null, "anonymous categories");

        // product monitoring: admin-view read, admin-write mutate
        expectAdminStatus(200, get("/api/admin/products"), tokens.get("ADMIN"), ADMIN_EMAIL, "admin lists products");
        expectAdminStatus(200, get("/api/admin/products"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator lists products");
        expectAdminStatus(200, get("/api/admin/products/stats"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator stats");
        expectAdminStatus(200, get("/api/admin/products/" + productX), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator product detail");
        expectAdminStatus(200, get("/api/admin/products/categories"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator categories");
        expectAdminDenied(get("/api/admin/products/stats"), tokens.get("FARM_A"), FARM_A_EMAIL, "farm reads product stats");
        expectAdminDenied(get("/api/admin/products/categories"), tokens.get("RETAIL_A"), RETAIL_A_EMAIL, "retailer reads admin categories");
        expectAdminDenied(put("/api/admin/products/" + productX + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"),
                tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator changes product status");
        expectDenied(get("/api/admin/products"), null, "anonymous admin products");
    }

    @Test
    void adminProductListing_withoutHeader_isStillRoleChecked() throws Exception {
        // SECURITY: /api/admin/products is an admin endpoint. Sending no X-Actor-Email must
        // not silently downgrade to the public ACTIVE catalogue for an authenticated
        // retailer/driver. It must be refused like every sibling admin endpoint.
        expectDenied(get("/api/admin/products"), tokens.get("RETAIL_A"), "retailer admin products, no header");
        expectDenied(get("/api/admin/products"), tokens.get("DRIVER_A"), "driver admin products, no header");
        expectDenied(get("/api/admin/products"), tokens.get("FARM_A"), "farm admin products, no header");
        // with their own (non-admin) header it is already 403
        expectAdminDenied(get("/api/admin/products"), tokens.get("RETAIL_A"), RETAIL_A_EMAIL, "retailer admin products, own header");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ORDERS — farm ⇄ retailer in both directions
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void orderLists_areScopedToTheCallersSide() throws Exception {
        // farm side
        JsonNode farmA = read(perform(get("/api/orders"), tokens.get("FARM_A")));
        List<Long> farmAIds = new ArrayList<>();
        farmA.forEach(o -> farmAIds.add(o.path("id").asLong()));
        assertTrue(farmAIds.contains(orderPendingX), "FARM_A must see orders for its product");
        JsonNode farmB = read(perform(get("/api/orders"), tokens.get("FARM_B")));
        List<Long> farmBIds = new ArrayList<>();
        farmB.forEach(o -> farmBIds.add(o.path("id").asLong()));
        assertFalse(farmBIds.contains(orderPendingX), "FARM_B must not see FARM_A's order");
        expectDenied(get("/api/orders"), tokens.get("RETAIL_A"), "retailer uses farm order list");
        expectDenied(get("/api/orders"), tokens.get("ADMIN"), "admin uses farm order list");

        // retailer side
        JsonNode retailB = read(perform(get("/api/orders/my"), tokens.get("RETAIL_B")));
        List<Long> retailBIds = new ArrayList<>();
        retailB.forEach(o -> retailBIds.add(o.path("id").asLong()));
        assertTrue(retailBIds.contains(orderPendingX), "RETAIL_B must see its own order");
        JsonNode retailA = read(perform(get("/api/orders/my"), tokens.get("RETAIL_A")));
        List<Long> retailAIds = new ArrayList<>();
        retailA.forEach(o -> retailAIds.add(o.path("id").asLong()));
        assertFalse(retailAIds.contains(orderPendingX), "RETAIL_A must not see RETAIL_B's order");
        expectDenied(get("/api/orders/my"), tokens.get("FARM_A"), "farm uses retailer order list");

        // detail: owning farm ok, peer farm denied, retailer denied on farm detail
        expectStatus(200, get("/api/orders/" + orderPendingX), tokens.get("FARM_A"), "owner farm order detail");
        expectDenied(get("/api/orders/" + orderPendingX), tokens.get("FARM_B"), "peer farm order detail");
        expectDenied(get("/api/orders/" + orderPendingX), tokens.get("RETAIL_B"), "retailer on farm order detail");
        // retailer detail: owner ok, other retailer denied
        expectStatus(200, get("/api/orders/my/" + orderPendingX), tokens.get("RETAIL_B"), "owner retailer order detail");
        expectDenied(get("/api/orders/my/" + orderPendingX), tokens.get("RETAIL_A"), "other retailer order detail");
    }

    @Test
    void orderAcceptReject_isOwningFarmOnly() throws Exception {
        // a retailer cannot accept/reject its own order, a peer farm cannot either
        expectDenied(put("/api/orders/" + orderPendingX + "/accept"), tokens.get("RETAIL_B"), "retailer accepts order");
        expectDenied(put("/api/orders/" + orderPendingX + "/accept"), tokens.get("FARM_B"), "peer farm accepts order");
        expectDenied(put("/api/orders/" + orderPendingX + "/accept"), tokens.get("ADMIN"), "admin accepts order");

        expectStatus(200, put("/api/orders/" + orderAcceptX + "/accept"), tokens.get("FARM_A"), "owner accepts order");

        // reject requires a reason, and only the owning farm may reject
        expectDenied(put("/api/orders/" + orderPendingX + "/reject").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("reason", "het hang"))), tokens.get("FARM_B"), "peer farm rejects order");
        expectStatus(400, put("/api/orders/" + orderPendingX + "/reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"\"}"), tokens.get("FARM_A"), "reject without reason");
        expectStatus(200, put("/api/orders/" + orderRejectX + "/reject").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("reason", "het hang"))), tokens.get("FARM_A"), "owner rejects order");
    }

    @Test
    void orderDeposit_isOwningRetailerOnly() throws Exception {
        expectStatus(200, postJson("/api/orders/deposit", Map.of("orderId", orderDepositX), "RETAIL_A"),
                "owner retailer initiates deposit");
        expectDenied(postJson("/api/orders/deposit", Map.of("orderId", orderDepositX), "RETAIL_B"),
                "other retailer steals deposit");
        expectDenied(postJson("/api/orders/deposit", Map.of("orderId", orderDepositX), "FARM_A"),
                "farm initiates deposit");
        expectDenied(postJson("/api/orders/deposit", Map.of("orderId", orderDepositX), "ADMIN"),
                "admin initiates deposit");
    }

    @Test
    void orderCancelComplete_areOwningRetailerOnly() throws Exception {
        expectDenied(put("/api/orders/" + orderCancelX + "/cancel").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("reason", "doi y"))), tokens.get("RETAIL_A"), "other retailer cancels");
        expectDenied(put("/api/orders/" + orderCancelX + "/cancel").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("reason", "doi y"))), tokens.get("FARM_A"), "farm cancels order");
        expectStatus(200, put("/api/orders/" + orderCancelX + "/cancel").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("reason", "doi y"))), tokens.get("RETAIL_B"), "owner cancels order");

        // farm-only fulfilment endpoints reject a retailer
        expectDenied(put("/api/orders/" + orderDepositX + "/in-transit"), tokens.get("RETAIL_A"), "retailer marks in-transit");
        expectDenied(put("/api/orders/" + orderDepositX + "/deliver"), tokens.get("RETAIL_A"), "retailer confirms delivery");
        expectDenied(put("/api/orders/" + orderDepositX + "/in-transit"), tokens.get("FARM_B"), "peer farm marks in-transit");

        // completion is a retailer action and only after delivery
        expectDenied(put("/api/orders/" + orderPendingX + "/complete"), tokens.get("FARM_A"), "farm completes order");
        expectDenied(put("/api/orders/" + orderPendingX + "/complete"), tokens.get("RETAIL_A"), "other retailer completes order");
        expectStatus(400, put("/api/orders/" + orderPendingX + "/complete"), tokens.get("RETAIL_B"), "complete before delivery");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SHIPPING MANAGER ⇄ everything
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void shippingEndpoints_areShippingManagerOnly() throws Exception {
        String[] reads = {
                "/api/shipping/orders/ready-to-ship",
                "/api/shipping/orders/completed",
                "/api/shipping/driver-reports",
                "/api/shipping/driver-users",
                "/api/shipping/shipments",
                "/api/shipping/vehicles",
                "/api/shipping/drivers"
        };
        for (String url : reads) {
            expectStatus(200, get(url), tokens.get("SHIPPING"), "shipping reads " + url);
            expectDenied(get(url), tokens.get("FARM_A"), "farm reads " + url);
            expectDenied(get(url), tokens.get("RETAIL_A"), "retailer reads " + url);
            expectDenied(get(url), tokens.get("DRIVER_A"), "driver reads " + url);
            expectDenied(get(url), tokens.get("ADMIN"), "admin reads " + url);
        }

        expectStatus(200, get("/api/shipping/shipments/" + shipmentX), tokens.get("SHIPPING"), "shipping shipment detail");
        expectDenied(get("/api/shipping/shipments/" + shipmentX), tokens.get("RETAIL_A"), "retailer shipping shipment detail");
        expectDenied(get("/api/shipping/shipments/" + shipmentX), tokens.get("DRIVER_B"), "driver uses shipping endpoint");

        // writes are shipping-manager only
        expectDenied(postJson("/api/shipping/vehicles", Map.of("licensePlate", "CR-NO-" + run,
                "type", "Xe", "capacity", 100.0), "FARM_A"), "farm creates vehicle");
        expectDenied(postJson("/api/shipping/vehicles", Map.of("licensePlate", "CR-NO2-" + run,
                "type", "Xe", "capacity", 100.0), "DRIVER_A"), "driver creates vehicle");
        expectStatus(201, postJson("/api/shipping/vehicles", Map.of("licensePlate", "CR-OK-" + run,
                "type", "Xe", "capacity", 100.0), "SHIPPING"), "shipping creates vehicle");
    }

    @Test
    void shipmentCreation_requiresShippingManager_andBusinessRules() throws Exception {
        long vehicleId = createVehicle("SHIPPING", "CR2-" + run);
        long driverBId = driverRepository.findByUserId(
                userRepository.findByEmail(DRIVER_B_EMAIL).orElseThrow().getId()).orElseThrow().getId();
        Map<String, Object> body = Map.of("orderId", orderDepositX, "driverId", driverBId,
                "vehicleId", vehicleId, "routeSummary", "CR route 2");

        expectDenied(postJson("/api/shipping/shipments", body, "FARM_A"), "farm creates shipment");
        expectDenied(postJson("/api/shipping/shipments", body, "RETAIL_A"), "retailer creates shipment");
        expectDenied(postJson("/api/shipping/shipments", body, "DRIVER_B"), "driver creates shipment");
        // orderDepositX is ACCEPTED (not DEPOSIT_PAID) → business error, not an auth bypass
        expectStatus(400, postJson("/api/shipping/shipments", body, "SHIPPING"), "shipment for unpaid order");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DRIVER ⇄ SHIPPING, driver ⇄ driver, full E2E
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void driverEndpoints_areAssignedDriverOnly_thenFullCrossRoleDelivery() throws Exception {
        // only SHIP_DRIVER role may use the driver app
        expectDenied(get("/api/driver/shipments"), tokens.get("FARM_A"), "farm uses driver app");
        expectDenied(get("/api/driver/shipments"), tokens.get("RETAIL_A"), "retailer uses driver app");
        expectDenied(get("/api/driver/shipments"), tokens.get("SHIPPING"), "shipping uses driver app");
        expectDenied(get("/api/driver/shipments"), tokens.get("ADMIN"), "admin uses driver app");
        expectDenied(get("/api/driver/shipments"), null, "anonymous uses driver app");

        // assigned driver sees the shipment, the other driver does not
        JsonNode driverB = read(perform(get("/api/driver/shipments"), tokens.get("DRIVER_B")));
        List<Long> driverBIds = new ArrayList<>();
        driverB.forEach(s -> driverBIds.add(s.path("id").asLong()));
        assertTrue(driverBIds.contains(shipmentX), "assigned driver must see the shipment");
        JsonNode driverA = read(perform(get("/api/driver/shipments"), tokens.get("DRIVER_A")));
        List<Long> driverAIds = new ArrayList<>();
        driverA.forEach(s -> driverAIds.add(s.path("id").asLong()));
        assertFalse(driverAIds.contains(shipmentX), "unassigned driver must not see the shipment");

        expectStatus(200, get("/api/driver/shipments/" + shipmentX), tokens.get("DRIVER_B"), "assigned driver detail");
        expectDenied(get("/api/driver/shipments/" + shipmentX), tokens.get("DRIVER_A"), "unassigned driver detail");
        expectDenied(postJson("/api/driver/shipments/" + shipmentX + "/pickup",
                Map.of("gpsLat", 10.9, "gpsLng", 106.8, "notes", "lay hang"), "DRIVER_A"), "unassigned driver pickup");

        // F11: pickup without the matching QR hash is rejected for the assigned driver
        expectStatus(400, postJson("/api/driver/shipments/" + shipmentX + "/pickup",
                Map.of("gpsLat", 10.9, "gpsLng", 106.8, "notes", "khong qr"), "DRIVER_B"), "pickup without QR");
        expectStatus(400, postJson("/api/driver/shipments/" + shipmentX + "/pickup",
                Map.of("gpsLat", 10.9, "gpsLng", 106.8, "notes", "sai qr", "traceHash", "0xwrong"), "DRIVER_B"),
                "pickup with wrong QR");

        // happy path with the scanned trace hash
        expectStatus(200, postJson("/api/driver/shipments/" + shipmentX + "/pickup",
                Map.of("gpsLat", 10.9, "gpsLng", 106.8, "notes", "da lay hang", "traceHash", traceX), "DRIVER_B"),
                "assigned driver pickup with QR");

        expectDenied(postJson("/api/driver/shipments/" + shipmentX + "/tracking",
                Map.of("status", "IN_TRANSIT", "gpsLat", 10.95, "gpsLng", 106.85), "DRIVER_A"), "unassigned driver tracking");
        expectStatus(200, postJson("/api/driver/shipments/" + shipmentX + "/tracking",
                Map.of("status", "IN_TRANSIT", "gpsLat", 10.95, "gpsLng", 106.85, "notes", "dang di"), "DRIVER_B"),
                "assigned driver tracking");
        // BR4: a GPS jump far from the previous checkpoint is rejected
        expectStatus(400, postJson("/api/driver/shipments/" + shipmentX + "/tracking",
                Map.of("status", "IN_TRANSIT", "gpsLat", 21.0, "gpsLng", 105.8, "notes", "nhay gps"), "DRIVER_B"),
                "BR4 GPS jump");

        // driver incident report: unassigned driver denied, assigned driver accepted
        expectDenied(postJson("/api/driver/reports",
                Map.of("shipmentId", shipmentX, "reportType", "DELAY", "description", "tre gio"), "DRIVER_A"),
                "unassigned driver report");
        expectStatus(200, postJson("/api/driver/reports",
                Map.of("shipmentId", shipmentX, "reportType", "DELAY", "description", "tre gio"), "DRIVER_B"),
                "assigned driver report");

        // delivery: only the assigned driver
        expectDenied(postJson("/api/driver/shipments/" + shipmentX + "/deliver",
                Map.of("gpsLat", 10.78, "gpsLng", 106.65), "DRIVER_A"), "unassigned driver deliver");
        expectStatus(200, postJson("/api/driver/shipments/" + shipmentX + "/deliver",
                Map.of("gpsLat", 10.78, "gpsLng", 106.65, "notes", "da giao"), "DRIVER_B"), "assigned driver deliver");

        // retailer only now completes; the other retailer cannot
        expectDenied(put("/api/orders/" + orderShipX + "/complete"), tokens.get("RETAIL_B"), "other retailer completes delivered order");
        expectStatus(200, put("/api/orders/" + orderShipX + "/complete"), tokens.get("RETAIL_A"), "owner retailer completes order");
        assertEquals("COMPLETED", read(perform(get("/api/orders/my/" + orderShipX), tokens.get("RETAIL_A")))
                .path("status").asText());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FARM SHIPMENTS VIEW — farm ⇄ shipping
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void farmShipmentViews_areTenantScoped() throws Exception {
        expectStatus(200, get("/api/farms/" + farmX + "/shipments"), tokens.get("FARM_A"), "owner lists farm shipments");
        expectStatus(200, get("/api/farms/" + farmX + "/shipments/summary"), tokens.get("FARM_A"), "owner shipment summary");
        expectDenied(get("/api/farms/" + farmX + "/shipments"), tokens.get("FARM_B"), "peer lists farm shipments");
        expectDenied(get("/api/farms/" + farmX + "/shipments/summary"), tokens.get("FARM_B"), "peer shipment summary");
        expectDenied(get("/api/farms/" + farmX + "/shipments"), tokens.get("SHIPPING"), "shipping uses farm shipment view");
        expectDenied(get("/api/farms/" + farmX + "/shipments"), tokens.get("RETAIL_A"), "retailer uses farm shipment view");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MARKETPLACE — retailer-only vs anonymous public
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void marketplace_isRetailerOnly_publicCatalogueIsAnonymous() throws Exception {
        expectStatus(200, get("/api/marketplace/products"), tokens.get("RETAIL_A"), "retailer searches marketplace");
        expectStatus(200, get("/api/marketplace/products/" + productX), tokens.get("RETAIL_A"), "retailer product detail");
        expectDenied(get("/api/marketplace/products"), tokens.get("FARM_A"), "farm searches marketplace");
        expectDenied(get("/api/marketplace/products"), tokens.get("DRIVER_A"), "driver searches marketplace");
        expectDenied(get("/api/marketplace/products"), null, "anonymous marketplace search");
        expectStatus(200, get("/api/marketplace/products/trace/" + traceX), null, "anonymous marketplace trace");

        expectStatus(200, get("/api/public/products"), null, "anonymous public catalogue");
        assertTrue(read(perform(get("/api/public/products"), null)).path("totalElements").asInt() > 0,
                "public catalogue exposes the approved ACTIVE product");
        expectStatus(200, get("/api/public/products/" + productX), null, "anonymous public product detail");
        expectStatus(200, get("/api/public/education"), null, "anonymous education list");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // IOT — farm owner vs peer vs other roles vs admin override
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void iotReadings_ownerOnly_andAdminsBypassOwnership() throws Exception {
        Map<String, Object> reading = Map.of("farmId", farmX, "temperature", 26.5, "humidity", 65.0, "ph", 6.4);
        expectStatus(200, postJson("/api/iot/sensors", reading, "FARM_A"), "owner pushes IoT reading");
        expectDenied(postJson("/api/iot/sensors", reading, "FARM_B"), "peer pushes IoT reading");
        expectDenied(postJson("/api/iot/sensors", reading, "RETAIL_A"), "retailer pushes IoT reading");
        expectDenied(postJson("/api/iot/sensors", reading, "DRIVER_A"), "driver pushes IoT reading");
        // documented behaviour: ADMIN/SUPER_ADMIN bypass the ownership check (IotDataServiceImpl)
        expectStatus(200, postJson("/api/iot/sensors", reading, "ADMIN"), "admin pushes IoT reading (bypass)");
        expectStatus(200, postJson("/api/iot/sensors", reading, "SUPERADMIN"), "superadmin pushes IoT reading (bypass)");

        // validation still applies
        expectStatus(400, postJson("/api/iot/sensors",
                Map.of("farmId", farmX, "temperature", 99.9, "humidity", 1.0, "ph", 14.0), "FARM_A"), "IoT out of range");
        expectStatus(400, postJson("/api/iot/sensors", Map.of("farmId", farmX), "FARM_A"), "IoT missing fields");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // REPORTS — any role submits, only admin handles
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void reports_anyRoleSubmits_onlyAdminHandles() throws Exception {
        Map<String, Object> report = Map.of("type", "INCIDENT", "subject", "Su co CR " + run,
                "content", "Noi dung bao cao cross-role day du tren muoi ky tu.");
        List<Long> reportIds = new ArrayList<>();
        for (String actor : List.of("FARM_A", "FARM_B", "RETAIL_A", "RETAIL_B", "SHIPPING", "DRIVER_A", "ADMIN")) {
            MvcResult r = postJson("/api/reports", report, actor);
            assertEquals(201, r.getResponse().getStatus(), () -> actor + " submits report body=" + safeBody(r));
            reportIds.add(read(r).path("id").asLong());
        }

        expectStatus(200, get("/api/reports/my"), tokens.get("FARM_A"), "farm lists own reports");
        expectDenied(get("/api/reports/admin"), tokens.get("FARM_A"), "farm reads admin reports");
        expectDenied(get("/api/reports/admin"), tokens.get("RETAIL_A"), "retailer reads admin reports");
        expectStatus(200, get("/api/reports/admin"), tokens.get("ADMIN"), "admin reads reports");
        expectStatus(200, get("/api/reports/admin"), tokens.get("MODERATOR"), "moderator reads reports");
        expectStatus(200, get("/api/reports/admin/stats"), tokens.get("MODERATOR"), "moderator report stats");
        expectStatus(200, get("/api/reports/admin/" + reportIds.get(0)), tokens.get("MODERATOR"), "moderator report detail");

        // handling is admin-write: a moderator may read but not resolve
        expectDenied(put("/api/reports/admin/" + reportIds.get(0) + "/handle").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("status", "RESOLVED", "adminResponse", "da xu ly"))),
                tokens.get("MODERATOR"), "moderator handles report");
        expectDenied(put("/api/reports/admin/" + reportIds.get(0) + "/handle").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("status", "RESOLVED", "adminResponse", "da xu ly"))),
                tokens.get("FARM_A"), "farm handles report");
        expectStatus(200, put("/api/reports/admin/" + reportIds.get(0) + "/handle").contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("status", "RESOLVED", "adminResponse", "da xu ly"))),
                tokens.get("ADMIN"), "admin handles report");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATIONS — guest vs user, ownership, broadcast role
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void notifications_guestVsUser_ownershipAndBroadcastRole() throws Exception {
        // anonymous feed = system announcements only, and still returns real data
        MvcResult anon = perform(get("/api/notifications"), null);
        assertEquals(200, anon.getResponse().getStatus());
        assertTrue(read(anon).path("notifications").size() > 0, "guest feed exposes platform announcements");

        expectStatus(200, get("/api/notifications"), tokens.get("FARM_A"), "farm notification feed");
        expectDenied(get("/api/notifications/unread-count"), null, "anonymous unread count");
        expectStatus(200, get("/api/notifications/unread-count"), tokens.get("FARM_A"), "farm unread count");

        // a user may not mark another user's notification as read
        long farmBUserId = userRepository.findByEmail(FARM_B_EMAIL).orElseThrow().getId();
        var foreign = notificationRepository.save(vn.courses.ut.edu.javaprogramming.bicap.entity.Notification.builder()
                .userId(farmBUserId).system(false).type("INFO").title("CR private " + run)
                .content("Tin nhan rieng cua FARM_B").channel("IN_APP").isRead(false).build());
        expectDenied(put("/api/notifications/" + foreign.getId() + "/read"), tokens.get("FARM_A"),
                "farm marks another user's notification read");
        expectStatus(200, put("/api/notifications/" + foreign.getId() + "/read"), tokens.get("FARM_B"),
                "owner marks own notification read");

        // broadcast is shipping-manager only
        Map<String, Object> broadcast = Map.of("target", "FARM_MANAGER", "title", "CR broadcast " + run,
                "content", "Noi dung broadcast cross-role tren muoi ky tu.", "sendEmail", false);
        expectDenied(postJson("/api/notifications/broadcast", broadcast, "FARM_A"), "farm broadcasts");
        expectDenied(postJson("/api/notifications/broadcast", broadcast, "RETAIL_A"), "retailer broadcasts");
        expectDenied(postJson("/api/notifications/broadcast", broadcast, "ADMIN"), "admin broadcasts");
        expectStatus(201, postJson("/api/notifications/broadcast", broadcast, "SHIPPING"), "shipping broadcasts");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SERVICE PACKAGES & SUBSCRIPTIONS — guest, admin, owner, peer
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void servicePackagesAndSubscriptions_roleMatrix() throws Exception {
        expectStatus(200, get("/api/service-packages"), null, "anonymous package list");
        expectStatus(200, get("/api/service-packages/admin/all"), tokens.get("ADMIN"), "admin all packages");
        expectStatus(200, get("/api/service-packages/admin/all"), tokens.get("MODERATOR"), "moderator all packages");
        expectDenied(get("/api/service-packages/admin/all"), tokens.get("FARM_A"), "farm all packages");
        expectDenied(get("/api/service-packages/admin/all"), null, "anonymous all packages");

        expectDenied(postJson("/api/service-packages/admin", Map.of("name", "Goi CR " + run,
                "description", "goi kiem thu", "price", "1000", "durationDays", "30"), "MODERATOR"),
                "moderator creates package");
        expectDenied(postJson("/api/service-packages/admin", Map.of("name", "Goi CR2 " + run,
                "description", "goi kiem thu", "price", "1000", "durationDays", "30"), "FARM_A"),
                "farm creates package");
        expectStatus(201, postJson("/api/service-packages/admin", Map.of("name", "Goi CR3 " + run,
                "description", "goi kiem thu", "price", "1000", "durationDays", "30"), "ADMIN"),
                "admin creates package");

        // subscription visibility: owner + admin-view, peer denied
        expectStatus(200, get("/api/subscriptions/farm/" + farmX), tokens.get("FARM_A"), "owner farm subscriptions");
        expectDenied(get("/api/subscriptions/farm/" + farmX), tokens.get("FARM_B"), "peer farm subscriptions");
        expectStatus(200, get("/api/subscriptions/farm/" + farmX), tokens.get("ADMIN"), "admin farm subscriptions");
        expectStatus(200, get("/api/subscriptions/my"), tokens.get("FARM_A"), "farm my subscriptions");
        // a non-owner cannot buy a package for someone else's farm
        long packageId = read(perform(get("/api/service-packages"), null)).get(0).path("id").asLong();
        expectDenied(postJson("/api/subscriptions/purchase",
                Map.of("packageId", packageId, "farmId", farmX), "FARM_B"), "peer buys package for farm");
        expectDenied(postJson("/api/subscriptions/purchase",
                Map.of("packageId", packageId, "farmId", farmX), "RETAIL_A"), "retailer buys package for farm");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BLOCKCHAIN & SMART CONTRACTS — admin-view vs admin-write vs superadmin
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void blockchainAndSmartContracts_adminRoleLadder() throws Exception {
        expectAdminStatus(200, get("/api/blockchain/transactions"), tokens.get("ADMIN"), ADMIN_EMAIL, "admin ledger");
        expectAdminStatus(200, get("/api/blockchain/transactions"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator ledger");
        expectAdminStatus(200, get("/api/blockchain/transactions"), tokens.get("SUPERADMIN"), SUPERADMIN_EMAIL, "superadmin ledger");
        expectAdminDenied(get("/api/blockchain/transactions"), tokens.get("FARM_A"), FARM_A_EMAIL, "farm ledger");
        expectAdminDenied(get("/api/blockchain/transactions"), tokens.get("RETAIL_A"), RETAIL_A_EMAIL, "retailer ledger");
        expectDenied(get("/api/blockchain/transactions"), null, "anonymous ledger");

        // retry is admin-write
        expectAdminDenied(post("/api/blockchain/transactions/999999/retry"), tokens.get("MODERATOR"), MODERATOR_EMAIL,
                "moderator retries tx");
        expectAdminDenied(post("/api/blockchain/transactions/999999/retry"), tokens.get("FARM_A"), FARM_A_EMAIL,
                "farm retries tx");
        MvcResult adminRetry = performAdmin(post("/api/blockchain/transactions/999999/retry"), tokens.get("ADMIN"), ADMIN_EMAIL);
        assertTrue(adminRetry.getResponse().getStatus() != 401 && adminRetry.getResponse().getStatus() != 403,
                () -> "admin retry should pass authz, got " + adminRetry.getResponse().getStatus());

        expectAdminStatus(200, get("/api/admin/contracts"), tokens.get("ADMIN"), ADMIN_EMAIL, "admin contracts");
        expectAdminStatus(200, get("/api/admin/contracts"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator contracts");
        expectAdminStatus(200, get("/api/admin/contracts/blockchain-status"), tokens.get("MODERATOR"), MODERATOR_EMAIL,
                "moderator blockchain status");
        expectAdminDenied(get("/api/admin/contracts"), tokens.get("FARM_A"), FARM_A_EMAIL, "farm contracts");

        // deploy is super-admin only
        Map<String, Object> deploy = Map.of("name", "CR Contract " + run, "bytecode", "0x6001",
                "abi", "[]", "environment", "TESTNET");
        expectAdminDenied(post("/api/admin/contracts/deploy").contentType(MediaType.APPLICATION_JSON).content(toJson(deploy)),
                tokens.get("ADMIN"), ADMIN_EMAIL, "admin deploys contract");
        expectAdminDenied(post("/api/admin/contracts/deploy").contentType(MediaType.APPLICATION_JSON).content(toJson(deploy)),
                tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator deploys contract");
        MvcResult superDeploy = performAdmin(post("/api/admin/contracts/deploy")
                .contentType(MediaType.APPLICATION_JSON).content(toJson(deploy)), tokens.get("SUPERADMIN"), SUPERADMIN_EMAIL);
        assertTrue(superDeploy.getResponse().getStatus() != 401 && superDeploy.getResponse().getStatus() != 403,
                () -> "superadmin deploy should pass authz, got " + superDeploy.getResponse().getStatus()
                        + " body=" + safeBody(superDeploy));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN ACCOUNT MANAGEMENT — view vs superadmin-only writes
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void adminAccountManagement_superAdminOnlyWrites() throws Exception {
        expectAdminStatus(200, get("/api/admins"), tokens.get("SUPERADMIN"), SUPERADMIN_EMAIL, "superadmin lists admins");
        expectAdminStatus(200, get("/api/admins"), tokens.get("ADMIN"), ADMIN_EMAIL, "admin lists admins");
        expectAdminStatus(200, get("/api/admins"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator lists admins");
        expectAdminStatus(200, get("/api/admins/permissions"), tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator permission catalogue");
        expectAdminDenied(get("/api/admins"), tokens.get("FARM_A"), FARM_A_EMAIL, "farm lists admins");
        expectAdminDenied(get("/api/admins"), tokens.get("RETAIL_A"), RETAIL_A_EMAIL, "retailer lists admins");
        expectDenied(get("/api/admins"), null, "anonymous lists admins");

        Map<String, Object> create = Map.of("fullName", "CR Admin " + run, "email", "cr.admin." + run + "@bicap.com",
                "password", "CrossRole@2026", "phone", "0900000000", "role", "ADMIN");
        expectAdminDenied(post("/api/admins").contentType(MediaType.APPLICATION_JSON).content(toJson(create)),
                tokens.get("ADMIN"), ADMIN_EMAIL, "admin creates admin");
        expectAdminDenied(post("/api/admins").contentType(MediaType.APPLICATION_JSON).content(toJson(create)),
                tokens.get("MODERATOR"), MODERATOR_EMAIL, "moderator creates admin");
        expectAdminStatus(201, post("/api/admins").contentType(MediaType.APPLICATION_JSON).content(toJson(create)),
                tokens.get("SUPERADMIN"), SUPERADMIN_EMAIL, "superadmin creates admin");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RETAILER PARTNERS + PROFILE + SHIPMENTS — farm ⇄ retailer
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void retailerPartnersAndProfile_roleMatrix() throws Exception {
        expectStatus(200, get("/api/retailers"), tokens.get("FARM_A"), "farm lists retailer partners");
        expectDenied(get("/api/retailers"), tokens.get("RETAIL_A"), "retailer lists retailer partners");
        expectDenied(get("/api/retailers"), tokens.get("ADMIN"), "admin lists retailer partners (farm-only)");
        expectDenied(get("/api/retailers"), tokens.get("DRIVER_A"), "driver lists retailer partners");

        expectStatus(200, get("/api/retailer/profile"), tokens.get("RETAIL_A"), "retailer own profile");
        expectDenied(get("/api/retailer/profile"), tokens.get("FARM_A"), "farm reads retailer profile");
        expectDenied(get("/api/retailer/profile"), tokens.get("DRIVER_A"), "driver reads retailer profile");
        expectStatus(200, get("/api/retailer/shipments"), tokens.get("RETAIL_A"), "retailer own shipments");
        expectDenied(get("/api/retailer/shipments"), tokens.get("FARM_A"), "farm reads retailer shipments");
        // No business profile is seeded for this retailer, so owner gets 404 (not 403/401).
        expectStatus(404, get("/api/retailer/business-profile"), tokens.get("RETAIL_A"), "retailer business profile");
        expectDenied(get("/api/retailer/business-profile"), tokens.get("FARM_A"), "farm reads retailer business profile");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROFILE self-service — any authenticated role, but only their own
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void profileSelfService_isPerAuthenticatedUser() throws Exception {
        expectStatus(200, get("/api/profile"), tokens.get("FARM_A"), "farm profile");
        expectStatus(200, get("/api/profile"), tokens.get("RETAIL_A"), "retailer profile");
        expectStatus(200, get("/api/profile"), tokens.get("DRIVER_A"), "driver profile");
        expectStatus(200, get("/api/profile"), tokens.get("SHIPPING"), "shipping profile");
        expectStatus(200, get("/api/profile"), tokens.get("ADMIN"), "admin profile");
        expectDenied(get("/api/profile"), null, "anonymous profile");

        // change-password validates the current password
        expectStatus(400, postJson("/api/profile/change-password", Map.of(
                "currentPassword", "SaiMatKhau@2026", "newPassword", "NewPassword@2026",
                "confirmPassword", "NewPassword@2026"), "FARM_A"), "change password wrong current");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CR-03: public package detail must not expose INACTIVE packages
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void servicePackageDetail_hidesInactiveFromPublic() throws Exception {
        String name = "Goi INACTIVE CR " + run;
        MvcResult created = postJson("/api/service-packages/admin",
                Map.of("name", name, "description", "goi an", "price", "1000", "durationDays", "30"), "ADMIN");
        assertEquals(201, created.getResponse().getStatus(), () -> "create package body=" + safeBody(created));
        long id = read(created).path("id").asLong();

        // while ACTIVE the public detail endpoint serves it to guests
        expectStatus(200, get("/api/service-packages/" + id), null, "public detail ACTIVE package");

        // admin deactivates it
        MvcResult deactivated = performAdmin(put("/api/service-packages/admin/" + id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"),
                tokens.get("ADMIN"), ADMIN_EMAIL);
        assertEquals(200, deactivated.getResponse().getStatus(), () -> "deactivate body=" + safeBody(deactivated));

        // public list and public detail both hide it; the admin list still shows it
        expectStatus(404, get("/api/service-packages/" + id), null, "public detail INACTIVE package");
        expectStatus(404, get("/api/service-packages/" + id), tokens.get("FARM_A"), "farm detail INACTIVE package");
        expectAdminStatus(200, get("/api/service-packages/admin/all"), tokens.get("ADMIN"), ADMIN_EMAIL,
                "admin all packages");
        boolean present = false;
        for (JsonNode n : read(performAdmin(get("/api/service-packages/admin/all"), tokens.get("ADMIN"), ADMIN_EMAIL))) {
            if (n.path("id").asLong() == id) {
                present = true;
            }
        }
        assertTrue(present, "an INACTIVE package must stay visible to admins via /admin/all");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CR-04: multipart validation must be 400, never 500
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void retailerProfile_multipartValidation_is400Not500() throws Exception {
        // missing required phone used to reach the service and NPE → 500
        expectStatus(400, multipart("/api/retailer/profile")
                .param("fullName", "Probe Retailer")
                .with(r -> { r.setMethod("PUT"); return r; }),
                tokens.get("RETAIL_A"), "profile without phone");
        // malformed phone fails the @Pattern
        expectStatus(400, multipart("/api/retailer/profile")
                .param("fullName", "Probe Retailer").param("phone", "123")
                .with(r -> { r.setMethod("PUT"); return r; }),
                tokens.get("RETAIL_A"), "profile with malformed phone");
        // non-retailer is still refused by role
        expectDenied(multipart("/api/retailer/profile")
                .param("fullName", "Probe").param("phone", "0987654322")
                .with(r -> { r.setMethod("PUT"); return r; }),
                tokens.get("FARM_A"), "farm updates retailer profile");
        // valid payload succeeds (unique phone: the seeded demo phones are already taken)
        String uniquePhone = "09" + String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
        MvcResult ok = perform(multipart("/api/retailer/profile")
                .param("fullName", "Probe Retailer").param("phone", uniquePhone)
                .with(r -> { r.setMethod("PUT"); return r; }), tokens.get("RETAIL_A"));
        assertEquals(200, ok.getResponse().getStatus(), () -> "valid profile body=" + safeBody(ok));

        // business documents: missing required fields → 400, not 500
        expectStatus(400, multipart("/api/retailer/documents")
                .param("businessName", "Probe Co").param("address", "addr").param("businessType", "RETAIL_STORE")
                .with(r -> { r.setMethod("POST"); return r; }),
                tokens.get("RETAIL_A"), "business docs without license");
        expectStatus(400, multipart("/api/retailer/business-profile")
                .param("businessName", "").param("address", "").param("businessType", "RETAIL_STORE")
                .with(r -> { r.setMethod("PUT"); return r; }),
                tokens.get("RETAIL_A"), "business docs blank fields");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CR-05: driver status is a closed set
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void driverUpdate_rejectsUnknownStatus() throws Exception {
        long driverAId = driverRepository.findByUserId(
                userRepository.findByEmail(DRIVER_A_EMAIL).orElseThrow().getId()).orElseThrow().getId();

        expectDenied(put("/api/shipping/drivers/" + driverAId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IDLE\"}"), tokens.get("FARM_A"), "farm updates a driver");
        expectStatus(400, put("/api/shipping/drivers/" + driverAId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BOGUS_STATUS\"}"), tokens.get("SHIPPING"), "unknown driver status");
        expectStatus(200, put("/api/shipping/drivers/" + driverAId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OFFLINE\"}"), tokens.get("SHIPPING"), "valid driver status OFFLINE");
        expectStatus(200, put("/api/shipping/drivers/" + driverAId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ON_TRIP\"}"), tokens.get("SHIPPING"), "restore driver ON_TRIP");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CR-07: ownership alone must not unlock farm modules without FARM_MANAGER
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void nonFarmRoleOwningAFarm_isStillDeniedFarmModules() throws Exception {
        // Simulate a mis-assigned/legacy account: a RETAILER that owns an APPROVED farm with an
        // active subscription. Every farm module must still require the FARM_MANAGER role.
        long retailerAId = userRepository.findByEmail(RETAIL_A_EMAIL).orElseThrow().getId();
        Farm owned = farmRepository.save(Farm.builder()
                .userId(retailerAId).name("CR Role Farm " + run).address("addr").area(1.0)
                .gpsLat(10.0).gpsLng(106.0).description("x").productTypes("y")
                .status(FarmStatus.APPROVED).build());
        long packageId = read(perform(get("/api/service-packages"), null)).get(0).path("id").asLong();
        Subscription sub = new Subscription();
        sub.setFarmId(owned.getId());
        sub.setPackageId(packageId);
        sub.setStartDate(LocalDate.now());
        sub.setEndDate(LocalDate.now().plusDays(365));
        sub.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(sub);

        expectDenied(get("/api/farms/" + owned.getId() + "/seasons"), tokens.get("RETAIL_A"),
                "retailer-owner lists seasons");
        expectDenied(postJson("/api/farms/" + owned.getId() + "/seasons", Map.of(
                        "name", "Vu la " + run, "productType", "Rau", "variety", "Cai", "area", 1.0,
                        "startDate", LocalDate.now().minusDays(1).toString()), "RETAIL_A"),
                "retailer-owner creates season");
        expectDenied(get("/api/farms/" + owned.getId() + "/exports"), tokens.get("RETAIL_A"),
                "retailer-owner lists exports");
        expectDenied(post("/api/farms/" + owned.getId() + "/seasons/999999/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Idempotency-Key", "cr-role-" + UUID.randomUUID())
                        .content(toJson(Map.of("quantity", 1, "unit", "kg",
                                "exportDate", LocalDate.now().toString(), "warehouse", "k"))),
                tokens.get("RETAIL_A"), "retailer-owner creates export");
        expectDenied(get("/api/subscriptions/farm/" + owned.getId()), tokens.get("RETAIL_A"),
                "retailer-owner reads subscriptions");
        expectDenied(postJson("/api/subscriptions/purchase",
                Map.of("packageId", packageId, "farmId", owned.getId()), "RETAIL_A"),
                "retailer-owner buys package");

        // a genuine FARM_MANAGER keeps working (control) — FARM_A owns seed farms
        expectStatus(200, get("/api/farms/" + farmX + "/seasons"), tokens.get("FARM_A"),
                "farm manager still lists seasons");
    }
}
