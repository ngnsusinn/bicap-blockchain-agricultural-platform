package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.EducationalContentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.EducationalContent;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.EducationalContentRepository;

import java.util.List;
import java.util.Locale;

/**
 * Read-only access to published educational content for the public (guest) area — F5.
 */
@Service
@Transactional(readOnly = true)
public class EducationService {

    private final EducationalContentRepository content;

    public EducationService(EducationalContentRepository content) {
        this.content = content;
    }

    public List<EducationalContentResponse> listPublished(String type, String keyword) {
        String status = EducationalContent.STATUS_PUBLISHED;
        String normalizedType = normalize(type);
        List<EducationalContent> items = normalizedType == null
                ? content.findByStatusOrderByPublishedAtDescCreatedAtDesc(status)
                : content.findByStatusAndTypeOrderByPublishedAtDescCreatedAtDesc(status, normalizedType);

        String q = normalize(keyword);
        return items.stream()
                .filter(item -> q == null
                        || contains(item.getTitle(), q)
                        || contains(item.getSummary(), q)
                        || contains(item.getTags(), q))
                .map(EducationalContentResponse::from)
                .toList();
    }

    public EducationalContentResponse getPublished(Long id) {
        return content.findByIdAndStatus(id, EducationalContent.STATUS_PUBLISHED)
                .map(EducationalContentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Educational content not found: " + id));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
