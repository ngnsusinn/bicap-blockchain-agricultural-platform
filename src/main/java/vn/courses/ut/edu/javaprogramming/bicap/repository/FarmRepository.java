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

    Optional<Farm> findByName(String name);

    Page<Farm> findByStatus(FarmStatus status, Pageable pageable);

    long countByStatus(FarmStatus status);

    /**
     * Search term is matched literally: callers must escape {@code !}, {@code %} and
     * {@code _} in the input with {@code !} (e.g. {@code % → !%}) so user input like
     * "%" or "_" is not interpreted as a LIKE wildcard. See {@code FarmApprovalService.escapeLike}.
     */
    @Query("SELECT DISTINCT f FROM Farm f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:search IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!' " +
           "OR LOWER(f.address) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!')")
    Page<Farm> findFarmsFiltered(
            @Param("status") FarmStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT f FROM Farm f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<Farm> findLatestByUserId(@Param("userId") Long userId, Pageable pageable);
}
