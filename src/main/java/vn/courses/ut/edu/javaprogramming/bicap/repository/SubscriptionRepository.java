package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Subscription;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByFarmId(Long farmId);
    Optional<Subscription> findByFarmIdAndStatus(Long farmId, String status);
}
