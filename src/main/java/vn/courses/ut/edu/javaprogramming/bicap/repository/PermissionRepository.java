package vn.courses.ut.edu.javaprogramming.bicap.repository;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
}
