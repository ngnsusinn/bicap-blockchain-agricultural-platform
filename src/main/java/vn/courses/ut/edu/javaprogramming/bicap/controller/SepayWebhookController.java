package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SepayWebhookRequest;
import vn.courses.ut.edu.javaprogramming.bicap.service.SepayService;

import java.util.HashMap;
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

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SepayWebhookRequest request) {
        
        Map<String, String> response = new HashMap<>();

        if (sepayConfig.getApiKey() != null && !sepayConfig.getApiKey().isEmpty()) {
            String expectedAuth = "Bearer " + sepayConfig.getApiKey();
            if (authHeader == null || !authHeader.equals(expectedAuth)) {
                response.put("success", "false");
                response.put("message", "Unauthorized webhook call");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }

        try {
            sepayService.handleWebhook(request);
            response.put("success", "true");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", "false");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}