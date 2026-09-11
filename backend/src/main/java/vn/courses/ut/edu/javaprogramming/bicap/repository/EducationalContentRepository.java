package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.EducationalContent;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationalContentRepository extends JpaRepository<EducationalContent, Long> {

    List<EducationalContent> findByStatusOrderByPublishedAtDescCreatedAtDesc(String status);

    List<EducationalContent> findByStatusAndTypeOrderByPublishedAtDescCreatedAtDesc(String status, String type);

    Optional<EducationalContent> findByIdAndStatus(Long id, String status);
}
