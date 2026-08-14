package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ExportStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SeasonExportRepository extends JpaRepository<SeasonExport, Long> {
    Optional<SeasonExport> findByIdempotencyKey(String idempotencyKey);
    Optional<SeasonExport> findByTraceHash(String traceHash);
    List<SeasonExport> findByFarmIdOrderByCreatedAtDesc(Long farmId);
    @Query("select coalesce(sum(e.quantity), 0) from SeasonExport e where e.seasonId = :seasonId and e.status <> :failed")
    BigDecimal sumCommittedQuantity(Long seasonId, ExportStatus failed);
}
