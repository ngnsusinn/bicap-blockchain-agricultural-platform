package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PaymentStatusResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PurchasePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SubscriptionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SubscriptionStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PAYMENT_CODE_DIGITS = 6;

    private final SubscriptionRepository subscriptionRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final FarmRepository farmRepository;
    private final SepayConfig sepayConfig;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               ServicePackageRepository servicePackageRepository,
                               FarmRepository farmRepository,
                               SepayConfig sepayConfig) {
        this.subscriptionRepository = subscriptionRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.farmRepository = farmRepository;
        this.sepayConfig = sepayConfig;
    }

    /**
     * Creates a PENDING_PAYMENT subscription for the authenticated user's farm and returns
     * the bank details + transfer memo code. Race protection (H-6): the DB unique constraint
     * on (farm_id, status) plus a catch on {@link DataIntegrityViolationException} makes a
     * concurrent double-purchase fail with 409 instead of creating two subscriptions.
     */
    public PurchasePackageResponse purchasePackage(PurchasePackageRequest request) {
        ServicePackage servicePackage = servicePackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found"));

        if (!"ACTIVE".equals(servicePackage.getStatus())) {
            throw new BadRequestException("Service package is not active");
        }

        User actor = CurrentUser.get();
        Farm farm = farmRepository.findByUserId(actor.getId()).stream().findFirst()
                .orElseThrow(() -> new BadRequestException("Farm Manager must register a farm before purchasing a package"));
        Long farmId = farm.getId();
        Subscription activeSubscription = subscriptionRepository.findByFarmIdAndStatus(farmId, SubscriptionStatus.ACTIVE)
                .orElse(null);
        expireSubscriptionIfNeeded(activeSubscription);

        if (subscriptionRepository.findByFarmIdAndStatus(farmId, SubscriptionStatus.PENDING_PAYMENT).isPresent()) {
            throw new ConflictException("Farm already has a pending payment — complete it before purchasing again");
        }
        if (activeSubscription != null && activeSubscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new ConflictException("Farm already has an active subscription");
        }

        Subscription subscription = Subscription.builder()
                .farmId(farmId)
                .packageId(request.getPackageId())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();
        try {
            subscription = subscriptionRepository.save(subscription);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Farm already has a pending payment — complete it before purchasing again");
        }

        String paymentCode = generateUniquePaymentCode(subscription.getId());
        subscription.setPaymentCode(paymentCode);
        subscription = subscriptionRepository.save(subscription);

        return new PurchasePackageResponse(
                subscription.getId(),
                paymentCode,
                sepayConfig.getBankName(),
                sepayConfig.getAccountNo(),
                servicePackage.getPrice(),
                paymentCode
        );
    }

    /** Returns the subscriptions of every farm the current user owns (no client-supplied farmId — M-2/M-5). */
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions() {
        User actor = CurrentUser.get();
        List<Farm> farms = farmRepository.findByUserId(actor.getId());
        if (farms.isEmpty()) {
            return List.of();
        }
        List<Subscription> subscriptions = farms.stream()
                .flatMap(f -> subscriptionRepository.findByFarmId(f.getId()).stream())
                .collect(Collectors.toList());
        subscriptions.forEach(this::expireSubscriptionIfNeeded);
        return toResponses(subscriptions);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByFarm(Long farmId) {
        checkAccessToFarm(farmId);
        return toResponses(subscriptionRepository.findByFarmId(farmId));
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse checkPaymentStatus(String paymentCode) {
        Subscription subscription = subscriptionRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResourceNotFoundException("Payment code not found or invalid format"));
        checkAccessToFarm(subscription.getFarmId());
        return new PaymentStatusResponse(paymentCode, subscription.getStatus().name(),
                subscription.getId(), "Status retrieved successfully");
    }

    /**
     * Activates a PENDING_PAYMENT subscription after a verified transfer. Guarded by a
     * state machine (H-5): only PENDING_PAYMENT may become ACTIVE, and the transferred
     * amount must cover the package price (C-4). Replay protection is handled by the
     * caller (SepayService) via idempotent txRef dedup.
     */
    public void activateSubscription(Long subscriptionId, BigDecimal transferAmount) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

        // State-machine guard FIRST — an already-active/cancelled subscription is rejected
        // before any package load or state mutation (H-5).
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new ConflictException("Subscription is already active");
        }
        if (subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Subscription is not in PENDING_PAYMENT status (current: "
                    + subscription.getStatus() + ")");
        }

        ServicePackage servicePackage = servicePackageRepository.findById(subscription.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found"));

        if (transferAmount == null || transferAmount.compareTo(servicePackage.getPrice()) < 0) {
            throw new BadRequestException("Transferred amount does not cover the package price");
        }

        // Compute start/end once from a single clock read (avoids midnight off-by-one).
        LocalDate today = LocalDate.now();
        subscription.setStartDate(today);
        subscription.setEndDate(today.plusDays(servicePackage.getDurationDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    // ── Internals ──

    private List<SubscriptionResponse> toResponses(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return List.of();
        }
        // Batch-load package names — one query for the whole list instead of one per row (M-10).
        Map<Long, String> namesById = servicePackageRepository
                .findAllById(subscriptions.stream().map(Subscription::getPackageId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ServicePackage::getId, ServicePackage::getName, (a, b) -> a));

        return subscriptions.stream()
                .map(sub -> new SubscriptionResponse(
                        sub.getId(),
                        sub.getFarmId(),
                        namesById.getOrDefault(sub.getPackageId(), "Unknown"),
                        sub.getStartDate(),
                        sub.getEndDate(),
                        sub.getStatus().name()))
                .collect(Collectors.toList());
    }

    private String generateUniquePaymentCode(Long subscriptionId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = "BICAP" + subscriptionId
                    + String.format("%0" + PAYMENT_CODE_DIGITS + "d", RANDOM.nextInt(1_000_000));
            if (subscriptionRepository.findByPaymentCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique payment code");
    }

    /** Keeps the persisted subscription state aligned with its configured validity period. */
    private void expireSubscriptionIfNeeded(Subscription subscription) {
        if (subscription != null
                && subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getEndDate() != null
                && subscription.getEndDate().isBefore(LocalDate.now())) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
        }
    }

    /** Throws unless the actor is an admin-view role or owns the given farm. */
    private void checkAccessToFarm(Long farmId) {
        User actor = CurrentUser.get();
        if (CurrentUser.isAdminView(actor)) {
            return;
        }
        boolean owns = farmRepository.findByUserId(actor.getId()).stream()
                .anyMatch(f -> f.getId().equals(farmId));
        if (!owns) {
            throw new ForbiddenException("You do not have access to this farm");
        }
    }
}
