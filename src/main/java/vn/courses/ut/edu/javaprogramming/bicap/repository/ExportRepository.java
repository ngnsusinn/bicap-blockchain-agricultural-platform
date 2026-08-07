package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Export;

import java.util.List;

@Repository
public interface ExportRepository extends JpaRepository<Export, Long> {
    List<Export> findBySeasonId(Long seasonId);
}
