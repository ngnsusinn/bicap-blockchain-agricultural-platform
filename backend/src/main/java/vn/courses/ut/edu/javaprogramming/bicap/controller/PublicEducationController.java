package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.EducationalContentResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.EducationService;

import java.util.List;

/**
 * Public educational content for guests (F5 — SRS role: Guest).
 *
 * <p>Anonymous access is granted through {@code /api/public/**} in {@code SecurityConfig};
 * only PUBLISHED items are ever returned.
 */
@RestController
@RequestMapping("/api/public/education")
public class PublicEducationController {

    private final EducationService educationService;

    public PublicEducationController(EducationService educationService) {
        this.educationService = educationService;
    }

    @GetMapping
    public ResponseEntity<List<EducationalContentResponse>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(educationService.listPublished(type, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EducationalContentResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(educationService.getPublished(id));
    }
}
