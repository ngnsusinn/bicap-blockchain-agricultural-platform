package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface FarmCertificationRepository extends JpaRepository<FarmCertification, Long> {

    List<FarmCertification> findByFarmId(Long farmId);

    /** Batch lookup for list endpoints — avoids one query per farm (N+1). */
    List<FarmCertification> findByFarmIdIn(Collection<Long> farmIds);
}
