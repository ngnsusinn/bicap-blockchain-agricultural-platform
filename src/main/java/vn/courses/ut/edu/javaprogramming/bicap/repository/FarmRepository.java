package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    Optional<Farm> findByUserId(Long userId);

    Page<Farm> findByStatus(FarmStatus status, Pageable pageable);

    long countByStatus(FarmStatus status);

    @Query("SELECT DISTINCT f FROM Farm f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(f.address) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Farm> findFarmsFiltered(
            @Param("status") FarmStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT f FROM Farm f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<Farm> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);
}
