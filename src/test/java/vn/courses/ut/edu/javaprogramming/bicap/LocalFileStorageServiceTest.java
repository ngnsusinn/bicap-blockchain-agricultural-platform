package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.service.LocalFileStorageService;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {
    @TempDir Path tempDir;

    @Test
    void storesAllowedAvatarInsideConfiguredRoot() {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
        MockMultipartFile avatar = new MockMultipartFile(
                "avatar", "avatar.png", "image/png", new byte[]{1, 2, 3});

        String url = service.storeAvatar(10L, avatar);

        assertTrue(url.startsWith("/uploads/retailers/10/avatars/"));
        assertTrue(url.endsWith(".png"));
    }

    @Test
    void rejectsPdfAsAvatar() {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile(
                "avatar", "avatar.pdf", "application/pdf", new byte[]{1});

        assertThrows(BadRequestException.class, () -> service.storeAvatar(10L, file));
    }

    @Test
    void acceptsPdfBusinessLicense() {
        LocalFileStorageService service = new LocalFileStorageService(tempDir.toString());
        MockMultipartFile license = new MockMultipartFile(
                "license", "license.pdf", "application/pdf", new byte[]{1});

        String url = service.storeBusinessLicense(10L, license);

        assertTrue(url.startsWith("/uploads/retailers/10/licenses/"));
        assertTrue(url.endsWith(".pdf"));
    }
}
