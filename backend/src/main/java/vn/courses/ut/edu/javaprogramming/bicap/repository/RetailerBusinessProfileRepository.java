package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RetailerBusinessProfileRepository extends JpaRepository<RetailerBusinessProfile, Long> {
    Optional<RetailerBusinessProfile> findByUserId(Long userId);

    /** Batch-load business profiles for several retailers (BICAP-21 partner listing, avoids N+1). */
    List<RetailerBusinessProfile> findByUserIdIn(Collection<Long> userIds);
}
