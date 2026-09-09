package vn.courses.ut.edu.javaprogramming.bicap.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PaymentStatusResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SubscriptionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.SubscriptionService;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchasePackageResponse> purchasePackage(@Valid @RequestBody PurchasePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.purchasePackage(request));
    }

    /** Subscriptions of the farms owned by the authenticated user (no client-supplied farmId). */
    @GetMapping("/my")
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

    /** Farm Manager: cancel an ACTIVE or PENDING_PAYMENT subscription (allows re-purchasing). */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long id) {
        subscriptionService.cancelSubscription(id);
        return ResponseEntity.noContent().build();
    }

    /** Farm Manager: cancel the current subscription belonging to the authenticated user. */
    @PutMapping("/current/cancel")
    public ResponseEntity<Void> cancelCurrentSubscription() {
        subscriptionService.cancelCurrentSubscription();
        return ResponseEntity.noContent().build();
    }
}
