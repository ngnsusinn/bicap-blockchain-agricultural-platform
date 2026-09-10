package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;

import java.util.List;

@Repository
public interface FarmingSeasonRepository extends JpaRepository<FarmingSeason, Long> {
    List<FarmingSeason> findByFarmId(Long farmId);
    org.springframework.data.domain.Page<FarmingSeason> findByFarmId(Long farmId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<FarmingSeason> findByFarmIdAndStatus(Long farmId, String status, org.springframework.data.domain.Pageable pageable);
}
