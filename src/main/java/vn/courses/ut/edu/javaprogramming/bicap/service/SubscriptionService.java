package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PaymentStatusResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SubscriptionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final SepayConfig sepayConfig;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ServicePackageRepository servicePackageRepository,
                               SepayConfig sepayConfig) {
        this.subscriptionRepository = subscriptionRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.sepayConfig = sepayConfig;
    }

    public PurchasePackageResponse purchasePackage(PurchasePackageRequest request) {
        ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found"));

        if (!"ACTIVE".equals(servicePackage.getStatus())) {
            throw new BadRequestException("Service package is not active");
        }

        Optional<Subscription> activeSubscription = subscriptionRepository.findByFarmIdAndStatus(request.getFarmId(), "ACTIVE");
        if (activeSubscription.isPresent()) {
            throw new ConflictException("Farm already has an active subscription");
        }

        Subscription subscription = new Subscription();
        subscription.setFarmId(request.getFarmId());
        subscription.setPackageId(request.getPackageId());
        subscription.setStatus("PENDING_PAYMENT");
        subscription.setStartDate(null);
        subscription.setEndDate(null);

        subscription = subscriptionRepository.save(subscription);

        String paymentCode = "BICAP" + subscription.getId() + String.format("%04d", new Random().nextInt(10000));
        
        return new PurchasePackageResponse(
                subscription.getId(),
                paymentCode,
                sepayConfig.getBankName(),
                sepayConfig.getAccountNo(),
                servicePackage.getPrice(),
                paymentCode
        );
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByFarm(Long farmId) {
        return subscriptionRepository.findByFarmId(farmId).stream().map(sub -> {
            ServicePackage servicePackage = servicePackageRepository.findById(sub.getPackageId()).orElse(null);
            String packageName = servicePackage != null ? servicePackage.getName() : "Unknown";
            return new SubscriptionResponse(sub.getId(), sub.getFarmId(), packageName, sub.getStartDate(), sub.getEndDate(), sub.getStatus());
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse checkPaymentStatus(String paymentCode) {
        Long subscriptionId = extractSubscriptionIdFromCode(paymentCode);
        if (subscriptionId == null) {
            throw new ResourceNotFoundException("Payment code not found or invalid format");
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        
        return new PaymentStatusResponse(paymentCode, subscription.getStatus(), subscription.getId(), "Status retrieved successfully");
    }

    public Long getSubscriptionIdByPaymentCode(String paymentCode) {
        return extractSubscriptionIdFromCode(paymentCode);
    }

    public void activateSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));
        ServicePackage servicePackage = servicePackageRepository.findById(subscription.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found"));

        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(servicePackage.getDurationDays()));
        subscription.setStatus("ACTIVE");
        subscriptionRepository.save(subscription);
    }

    private Long extractSubscriptionIdFromCode(String paymentCode) {
        if (paymentCode == null || !paymentCode.startsWith("BICAP") || paymentCode.length() <= 9) {
            return null;
        }
        try {
            String idStr = paymentCode.substring(5, paymentCode.length() - 4);
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}