package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FarmingProcessRepository extends JpaRepository<FarmingProcess, Long> {

    List<FarmingProcess> findBySeasonIdOrderByExecutionDateAsc(Long seasonId);

    long countBySeasonId(Long seasonId);
}
