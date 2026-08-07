package vn.courses.ut.edu.javaprogramming.bicap.repository;

<<<<<<< HEAD
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
=======
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;

>>>>>>> origin/main
import java.util.List;

@Repository
public interface FarmingProcessRepository extends JpaRepository<FarmingProcess, Long> {
<<<<<<< HEAD

    List<FarmingProcess> findBySeasonIdOrderByExecutionDateAsc(Long seasonId);

    long countBySeasonId(Long seasonId);
=======
    List<FarmingProcess> findBySeasonId(Long seasonId);
>>>>>>> origin/main
}
