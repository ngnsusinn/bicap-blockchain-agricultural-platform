package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Report;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    @Query("SELECT r FROM Report r " +
           "WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:type IS NULL OR r.type = :type) " +
           "AND (:reporterRole IS NULL OR r.reporterRole = :reporterRole) " +
           "ORDER BY r.createdAt DESC, r.id DESC")
    List<Report> findFiltered(@Param("status") String status,
                              @Param("type") String type,
                              @Param("reporterRole") String reporterRole);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status")
    long countByStatus(@Param("status") String status);
}
