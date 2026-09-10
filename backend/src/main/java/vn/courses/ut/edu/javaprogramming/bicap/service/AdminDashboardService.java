package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BlockchainTransaction;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ProductStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Report;
import vn.courses.ut.edu.javaprogramming.bicap.repository.BlockchainTransactionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ReportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate statistics for the Admin dashboard (EPIC-1 / detail-design §4.2 DashboardPage):
 * account, farm, product, order and report counters plus the pending-approval queue and
 * the latest blockchain activity. Read-only, visible to ADMIN_VIEW roles.
 */
@Service
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;
    private final BlockchainTransactionRepository blockchainRepository;
    private final FarmApprovalService farmApprovalService;

    public AdminDashboardService(UserRepository userRepository,
                                 FarmRepository farmRepository,
                                 ProductRepository productRepository,
                                 OrderRepository orderRepository,
                                 ReportRepository reportRepository,
                                 BlockchainTransactionRepository blockchainRepository,
                                 FarmApprovalService farmApprovalService) {
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reportRepository = reportRepository;
        this.blockchainRepository = blockchainRepository;
        this.farmApprovalService = farmApprovalService;
    }

    public Map<String, Object> getDashboard(String actorEmail) {
        ActorAuthorizer.requireAdminView(userRepository, actorEmail);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("admins", userRepository
                .findDistinctByRoles_NameIn(List.of("SUPER_ADMIN", "ADMIN", "MODERATOR")).size());
        result.put("farms", farmCounts());
        result.put("products", productCounts());
        result.put("orders", orderCounts());
        result.put("reports", reportCounts());
        result.put("pendingFarms", farmApprovalService
                .getFarms(FarmStatus.PENDING, null, PageRequest.of(0, 5), actorEmail).getContent());
        result.put("recentTransactions", blockchainRepository.findAll(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent());
        return result;
    }

    private Map<String, Long> farmCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (FarmStatus s : FarmStatus.values()) {
            m.put(s.name(), farmRepository.countByStatus(s));
        }
        m.put("TOTAL", farmRepository.count());
        return m;
    }

    private Map<String, Long> productCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (ProductStatus s : ProductStatus.values()) {
            m.put(s.name(), productRepository.countByStatus(s.name()));
        }
        m.put("TOTAL", productRepository.count());
        return m;
    }

    private Map<String, Long> orderCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            m.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        m.put("TOTAL", orderRepository.count());
        return m;
    }

    private Map<String, Long> reportCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put(Report.STATUS_OPEN, reportRepository.countByStatus(Report.STATUS_OPEN));
        m.put(Report.STATUS_IN_PROGRESS, reportRepository.countByStatus(Report.STATUS_IN_PROGRESS));
        m.put(Report.STATUS_RESOLVED, reportRepository.countByStatus(Report.STATUS_RESOLVED));
        m.put(Report.STATUS_REJECTED, reportRepository.countByStatus(Report.STATUS_REJECTED));
        m.put("TOTAL", reportRepository.count());
        return m;
    }
}
