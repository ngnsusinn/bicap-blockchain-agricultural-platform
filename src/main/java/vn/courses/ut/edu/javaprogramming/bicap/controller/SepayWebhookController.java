package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SepayWebhookRequest;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;
import vn.courses.ut.edu.javaprogramming.bicap.service.SepayService;

import java.util.Map;

@RestController
@RequestMapping("/api/public/sepay")
public class SepayWebhookController {

    private final SepayService sepayService;
    private final SepayConfig sepayConfig;

    public SepayWebhookController(SepayService sepayService, SepayConfig sepayConfig) {
        this.sepayService = sepayService;
        this.sepayConfig = sepayConfig;
    }

    /**
     * Fail-closed webhook (C-3): authentication is a strict {@code Bearer <apiKey>} comparison
     * against the configured key. When the key is empty or a placeholder, startup already
     * refused to boot (SecretConfigValidator), so an unconfigured webhook is unreachable.
     * Validation failures return 400/401 so the gateway retries and nothing is silently dropped.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SepayWebhookRequest request) {

        String expectedAuth = "Bearer " + sepayConfig.getApiKey();
        if (authHeader == null || !expectedAuth.equals(authHeader)) {
            throw new UnauthorizedException("Unauthorized webhook call");
        }

        return ResponseEntity.ok(sepayService.handleWebhook(request));
    }
}
