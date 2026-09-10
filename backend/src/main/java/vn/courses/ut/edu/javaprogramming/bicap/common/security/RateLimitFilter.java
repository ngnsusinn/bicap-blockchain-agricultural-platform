package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter for the publicly reachable auth endpoints (M-7).
 *
 * <p>Brute-force / credential-spraying protection for permitAll {@code /api/auth/**} without
 * an external dependency: per-IP counters in a bounded map, reset by a sliding window.
 * The limiter is deliberately simple (fixed window); swap for Bucket4j/Redis when the app
 * runs multiple instances.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 30;          // per window
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only protect the unauthenticated, credential-bearing endpoints.
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientKey(request);
        Bucket bucket = buckets.compute(key, (k, existing) -> {
            Bucket current = existing != null ? existing : new Bucket();
            if (current.expiresAt < System.currentTimeMillis()) {
                current.count = 0;
                current.expiresAt = System.currentTimeMillis() + WINDOW.toMillis();
            }
            return current;
        });

        if (bucket.count >= MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }
        bucket.count++;

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Reverse proxy chains: use the leftmost (client) address.
            int comma = forwarded.indexOf(',');
            return forwarded.substring(0, comma > 0 ? comma : forwarded.length()).trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }

    private static final class Bucket {
        int count;
        long expiresAt = System.currentTimeMillis();
    }
}
