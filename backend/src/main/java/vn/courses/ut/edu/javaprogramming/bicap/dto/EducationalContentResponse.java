package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.EducationalContent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public educational content payload (F5). {@code content} carries the article body;
 * for videos the playable URL is {@code videoUrl}.
 */
public record EducationalContentResponse(
        Long id,
        String title,
        String summary,
        String content,
        String type,
        String videoUrl,
        String coverImageUrl,
        List<String> tags,
        LocalDateTime publishedAt) {

    public static EducationalContentResponse from(EducationalContent value) {
        return new EducationalContentResponse(
                value.getId(),
                value.getTitle(),
                value.getSummary(),
                value.getContent(),
                value.getType(),
                value.getVideoUrl(),
                value.getCoverImageUrl(),
                parseTags(value.getTags()),
                value.getPublishedAt() != null ? value.getPublishedAt() : value.getCreatedAt());
    }

    private static List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}
