package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards client-side SPA routes to the bundled React shells so deep links and
 * page refreshes work when the frontends are served by Spring Boot (single port):
 *
 * <ul>
 *   <li>Farm Portal (Vite build) — static root {@code /}, deep link {@code /trace/{hash}}</li>
 *   <li>Admin Web (Vite build with base {@code /admin/}) — static {@code /admin/},
 *       portal routes {@code /admin/farm}, {@code /admin/retail}, {@code /admin/admin}</li>
 * </ul>
 *
 * <p>Patterns exclude segments containing a dot so real asset requests
 * ({@code /admin/assets/app.js}, {@code /favicon.svg}) keep hitting the static
 * resource handler instead of this controller. API endpoints live under
 * {@code /api/**} and are never matched here.
 */
@Controller
public class SpaForwardController {

    @RequestMapping({"/admin", "/admin/", "/admin/{segment:[^\\.]*}", "/admin/{segment:[^\\.]*}/{sub:[^\\.]*}"})
    public String forwardAdmin() {
        return "forward:/admin/index.html";
    }

    @RequestMapping("/trace/{hash:[^\\.]*}")
    public String forwardTrace() {
        return "forward:/index.html";
    }

    /**
     * Root: serve the Farm Portal shell when it has been bundled into the JAR
     * (build-web.bat). Without a frontend build, fall back to a plain status
     * message so a bare backend still answers {@code GET /}.
     */
    @GetMapping("/")
    public Object forwardRoot() {
        if (new ClassPathResource("static/index.html").exists()) {
            return "forward:/index.html";
        }
        return ResponseEntity.ok("BICAP - Blockchain Agricultural Platform is running! "
                + "(run build-web.bat to serve the Farm Portal here)");
    }
}
