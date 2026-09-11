package vn.courses.ut.edu.javaprogramming.bicap;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.RateLimitFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * M-7 / CR-06: the auth rate limiter must key on something the client cannot rotate.
 *
 * <p>{@code X-Forwarded-For} is a client-controlled header; trusting it by default let an
 * attacker send 30 requests, rotate the header, and never be limited. The filter now uses
 * the socket address unless the deployment explicitly declares a trusted reverse proxy.
 */
class RateLimitFilterTest {

    private static final FilterChain NOOP = (request, response) -> { };

    private MockHttpServletResponse hit(RateLimitFilter filter, String remoteAddr, String forwardedFor)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, NOOP);
        return response;
    }

    @Test
    void defaultConfig_ignoresClientSuppliedForwardedFor() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false);
        for (int i = 0; i < 30; i++) {
            // Rotate the spoofable header on every request; the socket address never changes.
            assertEquals(200, hit(filter, "10.0.0.1", "1.2.3." + i).getStatus(),
                    "request " + i + " should be allowed at or below the limit");
        }
        assertEquals(429, hit(filter, "10.0.0.1", "9.9.9.9").getStatus(),
                "the 31st request from the same socket address must be limited despite a new header");
    }

    @Test
    void otherSocketAddresses_haveIndependentBuckets() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false);
        for (int i = 0; i < 30; i++) {
            hit(filter, "10.0.0.1", null);
        }
        assertEquals(429, hit(filter, "10.0.0.1", null).getStatus());
        assertEquals(200, hit(filter, "10.0.0.2", null).getStatus(),
                "a different socket address must keep its own bucket");
    }

    @Test
    void trustedProxyMode_honoursForwardedFor() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true);
        // Reverse-proxy deployment: the proxy overwrites the header, so it identifies the client.
        for (int i = 0; i < 40; i++) {
            assertEquals(200, hit(filter, "127.0.0.1", "203.0.113." + i).getStatus(),
                    "distinct forwarded clients must not share a bucket");
        }
        for (int i = 0; i < 30; i++) {
            hit(filter, "127.0.0.1", "198.51.100.7");
        }
        assertEquals(429, hit(filter, "127.0.0.1", "198.51.100.7").getStatus(),
                "a single forwarded client is still limited");
    }
}
