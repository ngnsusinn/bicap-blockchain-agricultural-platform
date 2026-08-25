package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByExportId(Long exportId);

    /**
     * Search term is matched literally: callers must escape {@code !}, {@code %} and
     * {@code _} in the input with {@code !} so user input like "%" is not interpreted
     * as a LIKE wildcard (see {@code SearchUtils.escapeLike}).
     * The search covers the product name and the owning farm name (resolved through
     * FarmingSeason → Farm via explicit joins on the scalar id columns).
     */
    @Query("SELECT p FROM Product p " +
           "JOIN FarmingSeason s ON p.seasonId = s.id " +
           "JOIN Farm f ON s.farmId = f.id " +
           "WHERE (:status IS NULL OR p.status = :status) " +
           "AND (:categoryId IS NULL OR p.categoryId = :categoryId) " +
           "AND (:search IS NULL " +
           "     OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!' " +
           "     OR LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!')")
    Page<Product> findProductsFiltered(
            @Param("status") String status,
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable
    );

    long countByStatus(String status);

    @Query("SELECT p.categoryId, COUNT(p) FROM Product p GROUP BY p.categoryId")
    List<Object[]> countByCategory();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.createdAt >= :since")
    long countNewSince(@Param("since") LocalDateTime since);
}
