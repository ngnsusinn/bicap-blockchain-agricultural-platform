package vn.courses.ut.edu.javaprogramming.bicap.repository;

<<<<<<< HEAD
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
=======
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;

>>>>>>> origin/main
import java.util.List;

@Repository
public interface FarmingSeasonRepository extends JpaRepository<FarmingSeason, Long> {
<<<<<<< HEAD

    List<FarmingSeason> findByFarmIdOrderByCreatedAtDesc(Long farmId);

    Page<FarmingSeason> findByFarmId(Long farmId, Pageable pageable);

    Page<FarmingSeason> findByFarmIdAndStatus(Long farmId, SeasonStatus status, Pageable pageable);

    long countByFarmId(Long farmId);
=======
    List<FarmingSeason> findByFarmId(Long farmId);
>>>>>>> origin/main
}
