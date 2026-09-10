package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.IotData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IotDataRepository extends JpaRepository<IotData, Long> {
    List<IotData> findByFarmIdAndMeasuredAtBetween(Long farmId, LocalDateTime start, LocalDateTime end);
}
