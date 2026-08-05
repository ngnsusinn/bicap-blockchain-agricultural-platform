package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;

import java.util.Optional;

public interface RetailerBusinessProfileRepository extends JpaRepository<RetailerBusinessProfile, Long> {
    Optional<RetailerBusinessProfile> findByUserId(Long userId);
}
