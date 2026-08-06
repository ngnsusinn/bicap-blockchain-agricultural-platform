package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FarmingSeasonRepository extends JpaRepository<FarmingSeason, Long> {

    List<FarmingSeason> findByFarmIdOrderByCreatedAtDesc(Long farmId);

    Page<FarmingSeason> findByFarmId(Long farmId, Pageable pageable);

    Page<FarmingSeason> findByFarmIdAndStatus(Long farmId, SeasonStatus status, Pageable pageable);

    long countByFarmId(Long farmId);
}
