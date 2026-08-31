package vn.courses.ut.edu.javaprogramming.bicap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.time.LocalDate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BICAP-87 — end-to-end integration test across ALL modules on the real Spring context
 * (security filters + JWT + H2 + seeded roles):
 *
 * <pre>
 * Farm Manager registers → Admin approves farm → season + process (mock blockchain)
 * → harvest → export (QR/trace) → push product to trading floor → Admin approves product
 * → Retailer registers → searches marketplace → places order → Farm accepts → deposit
 * → Shipping Manager creates shipment (vehicle + driver) → Driver pickup/tracking/deliver
 * → Retailer confirms completion → Farm sends report → Admin handles report → notifications.
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
class FullLifecycleIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;
    @Autowired vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository subscriptionRepository;
    @Autowired vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository productRepository;
    @Autowired vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository driverRepository;

    private final Random rnd = new Random();

    private String suffix() { return String.valueOf(10000000 + rnd.nextInt(90000000)); }

    private JsonNode json(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString());
    }

    private String tokenOf(MvcResult r) throws Exception {
        return json(r).path("accessToken").asText();
    }

    private MvcResult postJson(String url, Object body, String bearer) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body));
        if (bearer != null) req.header("Authorization", "Bearer " + bearer);
        return mvc.perform(req).andReturn();
    }

    @Test
    void fullPlatformLifecycle() throws Exception {
        String s = suffix();

        // ── 1. Farm Manager registers + creates a farm ─────────────────────
        MvcResult farmAuth = mvc.perform(post("/api/auth/farm/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Tich Hop Farm","email":"e2e.farm%s@bicap.vn","phone":"09%s",
                         "password":"Farmpassword@2026","confirmPassword":"Farmpassword@2026"}"""
                        .formatted(s, s)))
                .andReturn();
        assertEquals(201, farmAuth.getResponse().getStatus(),
                "farm register body=" + farmAuth.getResponse().getContentAsString());
        String farmToken = tokenOf(farmAuth);

        MvcResult farmReg = postJson("/api/farms/register", java.util.Map.of(
                "name", "Trang Trai E2E " + s, "address", "Dong Nai", "area", 10.0,
                "gpsLat", 10.9, "gpsLng", 106.8, "description", "Vườn e2e", "productTypes", "rau"), farmToken);
        assertEquals(201, farmReg.getResponse().getStatus(), "body=" + farmReg.getResponse().getContentAsString());
        long farmId = json(farmReg).path("id").asLong();
        assertEquals("PENDING", json(farmReg).path("status").asText());

        // ── 2. Admin approves the farm (BICAP-3) ───────────────────────────
        String adminToken = tokenOf(postJson("/api/auth/admin/login",
                java.util.Map.of("identifier", "admin@bicap.com", "password", "Adminpassword@2026"), null));
        mvc.perform(put("/api/admin/farms/" + farmId + "/approve")
                        .header("X-Actor-Email", "admin@bicap.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // ── 2b. Activate a service package for the farm (purchase + simulated payment)
        long packageId = json(mvc.perform(get("/api/service-packages")).andReturn()).get(0).path("id").asLong();
        MvcResult purchase = postJson("/api/subscriptions/purchase",
                java.util.Map.of("packageId", packageId, "farmId", farmId), farmToken);
        assertTrue(purchase.getResponse().getStatus() < 300,
                "purchase body=" + purchase.getResponse().getContentAsString());
        subscriptionRepository.findByFarmIdAndStatus(farmId,
                        vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus.PENDING_PAYMENT)
                .ifPresent(sub -> {
                    sub.setStatus(vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus.ACTIVE);
                    subscriptionRepository.save(sub);
                });

        // ── 3. Season + process + harvest (BICAP-12→15, mock chain) ────────
        MvcResult season = postJson("/api/farms/" + farmId + "/seasons", java.util.Map.of(
                "name", "Vu E2E " + s, "productType", "Rau an la", "variety", "Cai xanh",
                "area", 5.0, "startDate", LocalDate.now().minusDays(30).toString()), farmToken);
        assertEquals(201, season.getResponse().getStatus());
        long seasonId = json(season).path("id").asLong();
        assertTrue(json(season).path("txHash").asText().startsWith("0x"), "season anchored on-chain (mock)");

        postJson("/api/seasons/" + seasonId + "/processes", java.util.Map.of(
                "processType", "SEEDING", "executionDate", LocalDate.now().minusDays(20).toString(),
                "materials", "hat giong", "notes", "gieo hat"), farmToken);

        MvcResult harvest = mvc.perform(patch("/api/farms/" + farmId + "/seasons/" + seasonId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + farmToken)
                        .content(om.writeValueAsString(java.util.Map.of(
                                "status", "HARVESTED", "harvestedQuantity", 100, "harvestUnit", "kg"))))
                .andReturn();
        assertEquals(200, harvest.getResponse().getStatus(),
                "harvest body=" + harvest.getResponse().getContentAsString());

        // ── 4. Export with QR (BICAP-16/17) ────────────────────────────────
        MvcResult export = mvc.perform(post("/api/farms/" + farmId + "/seasons/" + seasonId + "/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + farmToken)
                        .header("X-Idempotency-Key", "e2e-export-" + s)
                        .content(om.writeValueAsString(java.util.Map.of(
                                "quantity", 100, "unit", "kg",
                                "exportDate", LocalDate.now().toString(), "warehouse", "Kho A"))))
                .andReturn();
        assertTrue(export.getResponse().getStatus() < 300,
                "export body=" + export.getResponse().getContentAsString());
        long exportId = json(export).path("id").asLong();
        String traceHash = json(export).path("traceHash").asText();
        assertTrue(traceHash.startsWith("0x"), "trace hash issued");

        // ── 5. Push product to trading floor (BICAP-18) + admin approves (BICAP-5)
        long categoryId = json(mvc.perform(get("/api/categories")).andReturn()).get(0).path("id").asLong();
        MockMultipartFile image = new MockMultipartFile("images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile requestPart = new MockMultipartFile("request", "", "application/json",
                om.writeValueAsBytes(java.util.Map.of(
                        "exportId", exportId, "name", "Cai xanh E2E",
                        "description", "Rau cai xanh canh tac e2e kiem thong tin du dai toi thieu 50 ky tu theo quy dinh.",
                        "quantity", 50, "price", 15000, "categoryId", categoryId)));
        MvcResult listing = mvc.perform(multipart("/api/farms/" + farmId + "/marketplace/products")
                        .file(requestPart).file(image)
                        .header("Authorization", "Bearer " + farmToken))
                .andExpect(status().isCreated()).andReturn();
        long productId = json(listing).path("id").asLong();
        assertEquals("PENDING_REVIEW", json(listing).path("status").asText());

        mvc.perform(put("/api/admin/products/" + productId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
                        .header("X-Actor-Email", "admin@bicap.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertEquals("ACTIVE",
                productRepository.findById(productId).orElseThrow().getStatus(),
                "product should be ACTIVE after admin approval");

        // ── 6. Retailer registers, searches, orders (BICAP-36/39/42) ───────
        String retailToken = tokenOf(mvc.perform(post("/api/auth/retailer/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fullName":"Tich Hop Retail","email":"e2e.retail%s@bicap.vn","phone":"08%s",
                         "password":"Retailpassword@2026","confirmPassword":"Retailpassword@2026"}"""
                        .formatted(s, s)))
                .andReturn());

        MvcResult searchRes = mvc.perform(get("/api/marketplace/products")
                        .param("keyword", "Cai xanh E2E")
                        .header("Authorization", "Bearer " + retailToken)).andReturn();
        assertEquals(200, searchRes.getResponse().getStatus());
        assertEquals(1, json(searchRes).path("totalElements").asInt(),
                "search body=" + searchRes.getResponse().getContentAsString());

        MvcResult order = postJson("/api/orders", java.util.Map.of(
                "productId", productId, "quantity", 10.0, "deliveryAddr", "TP.HCM",
                "proposedPrice", 16000, "desiredDeliveryDate", LocalDate.now().plusDays(5).toString(),
                "notes", "don e2e"), retailToken);
        assertEquals(200, order.getResponse().getStatus());
        long orderId = json(order).path("id").asLong();

        // ── 7. Farm accepts, retailer deposits (BICAP-20/43) ───────────────
        mvc.perform(put("/api/orders/" + orderId + "/accept")
                        .header("Authorization", "Bearer " + farmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        postJson("/api/orders/deposit", java.util.Map.of("orderId", orderId), retailToken);
        // Simulate the Sepay webhook landing: order becomes DEPOSIT_PAID.
        Order paid = orderRepository.findById(orderId).orElseThrow();
        paid.setStatus(Order.STATUS_DEPOSIT_PAID);
        orderRepository.save(paid);

        // ── 8. Shipping Manager creates shipment (BICAP-55/76) ─────────────
        String shipToken = tokenOf(postJson("/api/auth/shipping/login",
                java.util.Map.of("identifier", "shipping_mgr@bicap.com", "password", "Shipping@2026"), null));
        MvcResult vehicle = postJson("/api/shipping/vehicles", java.util.Map.of(
                "licensePlate", "51H-" + s, "type", "Tai 500kg", "capacity", 500.0), shipToken);
        long vehicleId = json(vehicle).path("id").asLong();
        long driverUserId = userRepository.findByEmail("driver@bicap.com").orElseThrow().getId();
        long driverId = driverRepository.findByUserId(driverUserId).orElseThrow().getId();

        MvcResult shipment = postJson("/api/shipping/shipments", java.util.Map.of(
                "orderId", orderId, "driverId", driverId, "vehicleId", vehicleId,
                "routeSummary", "Dong Nai -> TP.HCM"), shipToken);
        assertEquals(201, shipment.getResponse().getStatus(),
                "shipment body=" + shipment.getResponse().getContentAsString()
                        + " | vehicle=" + vehicle.getResponse().getContentAsString());
        long shipmentId = json(shipment).path("id").asLong();

        // ── 9. Driver pickup → tracking → delivery (BICAP-64/66/67) ────────
        String driverToken = tokenOf(postJson("/api/auth/driver/login",
                java.util.Map.of("identifier", "driver@bicap.com", "password", "Driver@2026"), null));
        postJson("/api/driver/shipments/" + shipmentId + "/pickup",
                java.util.Map.of("gpsLat", 10.9, "gpsLng", 106.8, "notes", "Da lay hang"), driverToken);
        postJson("/api/driver/shipments/" + shipmentId + "/tracking",
                java.util.Map.of("status", "IN_TRANSIT", "gpsLat", 11.2, "gpsLng", 106.5, "notes", "tren duong"), driverToken);
        postJson("/api/driver/shipments/" + shipmentId + "/deliver",
                java.util.Map.of("gpsLat", 10.78, "gpsLng", 106.65, "notes", "Da giao"), driverToken);

        // ── 10. Retailer confirms completion (BICAP-51) ────────────────────
        mvc.perform(put("/api/orders/" + orderId + "/complete")
                        .header("Authorization", "Bearer " + retailToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // ── 11. Farm report → Admin handles (BICAP-27) ─────────────────────
        MvcResult report = postJson("/api/reports", java.util.Map.of(
                "type", "INCIDENT", "subject", "Su co e2e",
                "content", "Bao cao su co kiem thong tin e2e day du theo yeu cau."), farmToken);
        assertEquals(201, report.getResponse().getStatus());
        long reportId = json(report).path("id").asLong();

        mvc.perform(get("/api/reports/admin").header("X-Actor-Email", "admin@bicap.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(put("/api/reports/admin/" + reportId + "/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"adminResponse\":\"Da xu ly e2e\"}")
                        .header("X-Actor-Email", "admin@bicap.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // ── 12. Notifications reached the parties (BICAP-77) ───────────────
        MvcResult farmNotifs = mvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + farmToken)).andReturn();
        assertTrue(json(farmNotifs).path("unreadCount").asLong() > 0,
                "farm received in-app notifications");
    }
}
