package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png");
    private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

    private final Path root;

    public LocalFileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String storeAvatar(Long userId, MultipartFile file) {
        return store("retailers", userId, "avatars", file, IMAGE_TYPES, 5L * 1024 * 1024);
    }

    public String storeBusinessLicense(Long userId, MultipartFile file) {
        return store("retailers", userId, "licenses", file, DOCUMENT_TYPES, 10L * 1024 * 1024);
    }

    /** Stores a product marketplace image under {@code uploads/farms/{userId}/products/} (BICAP-18). */
    public String storeProductImage(Long userId, MultipartFile file) {
        return store("farms", userId, "products", file, IMAGE_TYPES, 5L * 1024 * 1024);
    }

    private String store(String topLevel, Long userId, String category, MultipartFile file,
                         Set<String> allowedTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is required");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowedTypes.contains(contentType)) {
            throw new BadRequestException("Unsupported file type");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Uploaded file exceeds the allowed size");
        }

        String extension = switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/png" -> ".png";
            default -> ".jpg";
        };
        String filename = UUID.randomUUID() + extension;
        Path directory = root.resolve(topLevel).resolve(userId.toString()).resolve(category).normalize();
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Invalid upload path");
        }
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + topLevel + "/" + userId + "/" + category + "/" + filename;
        } catch (IOException exception) {
            throw new BadRequestException("Could not store uploaded file");
        }
    }
}
