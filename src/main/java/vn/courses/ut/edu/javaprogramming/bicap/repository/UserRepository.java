package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCaseOrPhone(String email, String phone);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    List<User> findDistinctByRoles_NameIn(Collection<String> roleNames);

    /**
     * Search term is matched literally: callers must escape {@code !}, {@code %} and
     * {@code _} with {@code !} (see {@link vn.courses.ut.edu.javaprogramming.bicap.common.util.SearchUtils}).
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN u.roles r WHERE " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:role IS NULL OR LOWER(r.name) = LOWER(:role)) AND " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!' " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '!' " +
           "OR u.phone LIKE CONCAT('%', :search, '%') ESCAPE '!')")
    Page<User> findAdminsFiltered(
            @Param("status") UserStatus status,
            @Param("role") String role,
            @Param("search") String search,
            Pageable pageable
    );
}
