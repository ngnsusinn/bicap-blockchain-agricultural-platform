package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards client-side routes of the unified web app to its single React shell
 * so deep links and page refreshes work when the app is served by Spring Boot
 * (single port). The app is split by endpoint inside React Router-less routing
 * (`web/src/App.tsx`):
 *
 * <ul>
 *   <li>{@code /} + {@code /trace/{hash}} → Farm / Retailer / Shipping / Guest portal</li>
 *   <li>{@code /admin/**} → Admin dashboard</li>
 * </ul>
 *
 * <p>Patterns exclude segments containing a dot so real asset requests
 * ({@code /assets/app.js}, {@code /favicon.svg}) keep hitting the static
 * resource handler instead of this controller. API endpoints live under
 * {@code /api/**} and are never matched here.
 */
@Controller
public class SpaForwardController {

    @RequestMapping({"/admin", "/admin/", "/admin/{segment:[^\\.]*}", "/admin/{segment:[^\\.]*}/{sub:[^\\.]*}"})
    public String forwardAdmin() {
        return "forward:/index.html";
    }

    @RequestMapping("/trace/{hash:[^\\.]*}")
    public String forwardTrace() {
        return "forward:/index.html";
    }

    /**
     * Root: serve the web app shell when it has been bundled into the JAR.
     * Without a build, fall back to a plain status message so a bare backend
     * still answers {@code GET /}.
     */
    @GetMapping("/")
    public Object forwardRoot() {
        if (new ClassPathResource("static/index.html").exists()) {
            return "forward:/index.html";
        }
        return ResponseEntity.ok("BICAP - Blockchain Agricultural Platform is running! "
                + "(build the web/ app and copy it to src/main/resources/static to serve it here)");
    }
}
