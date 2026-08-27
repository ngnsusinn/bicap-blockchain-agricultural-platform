package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PaymentStatusResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SubscriptionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/purchase")
    @PreAuthorize("hasRole('FARM_MANAGER')")
    public ResponseEntity<PurchasePackageResponse> purchasePackage(@Valid @RequestBody PurchasePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.purchasePackage(request));
    }

    /** Subscriptions of the farms owned by the authenticated user (no client-supplied farmId). */
    @GetMapping("/my")
    @PreAuthorize("hasRole('FARM_MANAGER')")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions() {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions());
    }

    @GetMapping("/farm/{farmId}")
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptionsByFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByFarm(farmId));
    }

    @GetMapping("/payment-status/{paymentCode}")
    public ResponseEntity<PaymentStatusResponse> checkPaymentStatus(@PathVariable String paymentCode) {
        return ResponseEntity.ok(subscriptionService.checkPaymentStatus(paymentCode));
    }
}
