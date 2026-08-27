package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import java.util.List;
import java.util.Optional;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    List<ServicePackage> findAllByStatus(String status);
    Optional<ServicePackage> findByName(String name);
}
