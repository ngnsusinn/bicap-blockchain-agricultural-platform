package vn.courses.ut.edu.javaprogramming.bicap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Notification;
import vn.courses.ut.edu.javaprogramming.bicap.repository.NotificationRepository;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Regression suite for the audit findings C-1…C-4, F1, F3, F5, F8, F12.
 *
 * <p>Every assertion here corresponds to a defect that was reproduced live against a
 * running server before the fix, so a regression immediately re-opens a real hole:
 * <ul>
 *   <li>C-1 — a JWT forged with the repository's old default secret must be rejected.</li>
 *   <li>C-3 — the admin product/category endpoints and the guest notification feed must
 *       not be reachable anonymously.</li>
 *   <li>C-4 — only the owning farm (or an admin) may push IoT readings.</li>
 *   <li>F3/F5 — the guest catalogue and education content are real, anonymous endpoints.</li>
 *   <li>F8 — ready-to-ship and completed are distinct, correctly-labelled lists.</li>
 *   <li>F11/F12 — pickup requires a matching trace QR; legacy exports are tenant-scoped.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAndGuestRegressionIntegrationTest {

    /** application.properties default before the C-1 fix (published in the repo). */
    private static final String SHIPPED_JWT_DEFAULT = "dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw==";

    private static final Map<String, String> TOKEN_CACHE = new ConcurrentHashMap<>();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired NotificationRepository notificationRepository;

    private String token(String loginPath, String identifier, String password) throws Exception {
        String key = loginPath + "|" + identifier;
        String cached = TOKEN_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        MvcResult result = mvc.perform(post("/api/auth/" + loginPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("identifier", identifier, "password", password))))
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        String accessToken = body.path("accessToken").asText();
        assertFalse(accessToken.isBlank(), "login failed for " + identifier + ": " + body);
        TOKEN_CACHE.put(key, accessToken);
        return accessToken;
    }

    private String adminToken() throws Exception {
        return token("admin/login", "admin@bicap.com", "Adminpassword@2026");
    }

    // ── C-1 ─────────────────────────────────────────────────────────────────────

    @Test
    void forgedTokenSignedWithShippedDefaultSecret_isRejected() throws Exception {
        String forged = Jwts.builder()
                .subject("superadmin@bicap.com")
                .claims(Map.of("type", "access"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SHIPPED_JWT_DEFAULT)))
                .compact();

        mvc.perform(get("/api/admins")
                        .header("Authorization", "Bearer " + forged)
                        .header("X-Actor-Email", "superadmin@bicap.com"))
                .andExpect(status().is4xxClientError());
    }

    // ── C-3 ─────────────────────────────────────────────────────────────────────

    @Test
    void anonymousAdminProductListing_isBlocked() throws Exception {
        mvc.perform(get("/api/admin/products")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/admin/products").param("status", "PENDING_REVIEW"))
                .andExpect(status().is4xxClientError());
        // Spoofing an admin email on an anonymous request must not unlock the admin view.
        mvc.perform(get("/api/admin/products").header("X-Actor-Email", "superadmin@bicap.com"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void anonymousAdminCategoryListing_isClientErrorNotServerError() throws Exception {
        mvc.perform(get("/api/admin/products/categories"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void guestFeed_containsOnlySystemAnnouncements() throws Exception {
        Notification privateNotification = notificationRepository.save(Notification.builder()
                .userId(6L)
                .system(false)
                .type("INFO")
                .title("PRIVATE-REGRESSION-CHECK")
                .content("Tin nhắn riêng giữa nông trại và nhà bán lẻ — không được lộ cho khách.")
                .channel("IN_APP")
                .isRead(false)
                .build());

        MvcResult result = mvc.perform(get("/api/notifications")).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();

        assertFalse(body.contains("PRIVATE-REGRESSION-CHECK"),
                "guest feed must never expose another user's private notification");
        JsonNode payload = om.readTree(body);
        assertTrue(payload.path("notifications").size() > 0,
                "guest feed must still expose the seeded platform announcements");
        for (JsonNode notification : payload.path("notifications")) {
            assertNotEquals("PRIVATE-REGRESSION-CHECK", notification.path("title").asText());
        }

        notificationRepository.delete(privateNotification);
    }

    // ── C-4 ─────────────────────────────────────────────────────────────────────

    @Test
    void iot_retailerCannotInjectReadings() throws Exception {
        String retailer = token("retailer/login", "retailer@bicap.com", "Retailpassword@2026");
        // Plausible values so this exercises the AUTHORIZATION check, not payload validation.
        mvc.perform(post("/api/iot/sensors")
                        .header("Authorization", "Bearer " + retailer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":3,\"temperature\":30.0,\"humidity\":60.0,\"ph\":6.5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void iot_driverCannotInjectReadingsForAnotherFarm() throws Exception {
        String driver = token("driver/login", "driver@bicap.com", "Driver@2026");
        mvc.perform(post("/api/iot/sensors")
                        .header("Authorization", "Bearer " + driver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":3,\"temperature\":30.0,\"humidity\":60.0,\"ph\":6.5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void iot_outOfRangeValuesAreRejected() throws Exception {
        String owner = token("farm/login", "farm@bicap.com", "Farmpassword@2026");
        mvc.perform(post("/api/iot/sensors")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":3,\"temperature\":99.9,\"humidity\":1.0,\"ph\":14.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void iot_ownerCanPushForOwnFarm_butNotForSomeoneElses() throws Exception {
        String owner = token("farm/login", "farm@bicap.com", "Farmpassword@2026");
        mvc.perform(post("/api/iot/sensors")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":3,\"temperature\":26.5,\"humidity\":65.0,\"ph\":6.4}"))
                .andExpect(status().isOk());

        String otherFarm = token("farm/login", "farm@bicap.vn", "Farmpassword@2026");
        mvc.perform(post("/api/iot/sensors")
                        .header("Authorization", "Bearer " + otherFarm)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":3,\"temperature\":26.5,\"humidity\":65.0,\"ph\":6.4}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void iot_missingFields_isValidationError() throws Exception {
        String owner = token("farm/login", "farm@bicap.com", "Farmpassword@2026");
        mvc.perform(post("/api/iot/sensors")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"farmId\":3}"))
                .andExpect(status().isBadRequest());
    }

    // ── F3 / F5 ─────────────────────────────────────────────────────────────────

    @Test
    void publicCatalogue_isAnonymous_andReturnsRealData() throws Exception {
        MvcResult result = mvc.perform(get("/api/public/products"))
                .andExpect(status().isOk()).andReturn();
        JsonNode page = om.readTree(result.getResponse().getContentAsString());
        assertTrue(page.path("totalElements").asInt() > 0, "seeded ACTIVE product must be visible to guests");
        JsonNode first = page.path("content").get(0);
        assertTrue(first.hasNonNull("farmName"));
        assertTrue(first.hasNonNull("farmAddress"), "origin must come from the farm, not be fabricated");
        assertTrue(first.has("certifications"), "certifications must be real fields");
        assertTrue(first.has("traceHash"));
    }

    @Test
    void publicCatalogue_detailAndTraceAreAnonymous() throws Exception {
        MvcResult list = mvc.perform(get("/api/public/products")).andReturn();
        long id = om.readTree(list.getResponse().getContentAsString()).path("content").get(0).path("id").asLong();
        mvc.perform(get("/api/public/products/" + id)).andExpect(status().isOk());
        mvc.perform(get("/api/public/products/trace/0xdeadbeef")).andExpect(status().isNotFound());
    }

    @Test
    void publicEducation_isAnonymous_andHasArticlesAndVideos() throws Exception {
        MvcResult result = mvc.perform(get("/api/public/education"))
                .andExpect(status().isOk()).andReturn();
        JsonNode items = om.readTree(result.getResponse().getContentAsString());
        assertTrue(items.size() >= 4, "seeded education content must be served by the API");
        boolean hasArticle = false;
        boolean hasVideo = false;
        for (JsonNode item : items) {
            hasArticle |= "ARTICLE".equals(item.path("type").asText());
            hasVideo |= "VIDEO".equals(item.path("type").asText());
            assertFalse(item.path("content").asText().isBlank());
        }
        assertTrue(hasArticle && hasVideo);
        mvc.perform(get("/api/public/education").param("type", "VIDEO")).andExpect(status().isOk());
    }

    // ── F8 ──────────────────────────────────────────────────────────────────────

    @Test
    void shippingReadyToShipAndCompleted_areDistinctEndpoints() throws Exception {
        String shippingManager = token("shipping/login", "shipping_mgr@bicap.com", "Shipping@2026");
        mvc.perform(get("/api/shipping/orders/ready-to-ship")
                        .header("Authorization", "Bearer " + shippingManager))
                .andExpect(status().isOk());
        mvc.perform(get("/api/shipping/orders/completed")
                        .header("Authorization", "Bearer " + shippingManager))
                .andExpect(status().isOk());
    }

    // ── F11 ─────────────────────────────────────────────────────────────────────

    @Test
    void driverPickup_requiresScannedTraceHash() throws Exception {
        String driver = token("driver/login", "driver@bicap.com", "Driver@2026");
        MvcResult shipments = mvc.perform(get("/api/driver/shipments")
                        .header("Authorization", "Bearer " + driver))
                .andExpect(status().isOk()).andReturn();
        JsonNode list = om.readTree(shipments.getResponse().getContentAsString());
        long pickingUpId = -1;
        for (JsonNode shipment : list) {
            if ("PICKING_UP".equals(shipment.path("status").asText())) {
                pickingUpId = shipment.path("id").asLong();
                break;
            }
        }
        assertTrue(pickingUpId > 0, "seeded PICKING_UP shipment must exist");

        mvc.perform(post("/api/driver/shipments/" + pickingUpId + "/pickup")
                        .header("Authorization", "Bearer " + driver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gpsLat\":21.1,\"gpsLng\":105.6,\"notes\":\"khong quet QR\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── F12 ─────────────────────────────────────────────────────────────────────

    @Test
    void legacyExportListing_isTenantScoped() throws Exception {
        // Season 1 belongs to farm 3 (owner farm@bicap.com); farm@bicap.vn must not read it.
        String otherFarm = token("farm/login", "farm@bicap.vn", "Farmpassword@2026");
        mvc.perform(get("/api/seasons/1/exports").header("Authorization", "Bearer " + otherFarm))
                .andExpect(status().isForbidden());

        String owner = token("farm/login", "farm@bicap.com", "Farmpassword@2026");
        mvc.perform(get("/api/seasons/1/exports").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        String admin = adminToken();
        mvc.perform(get("/api/seasons/1/exports")
                        .header("Authorization", "Bearer " + admin)
                        .header("X-Actor-Email", "admin@bicap.com"))
                .andExpect(status().isOk());
    }
}
