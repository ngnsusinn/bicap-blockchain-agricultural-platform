package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PaymentStatusResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SubscriptionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchasePackageResponse> purchasePackage(@Valid @RequestBody PurchasePackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.purchasePackage(request));
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
