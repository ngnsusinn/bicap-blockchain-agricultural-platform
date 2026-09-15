package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.SecureSecretInitializer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * C-1 / C-3 regression: no secret may be missing, guessed or generated at runtime.
 *
 * <p>Before the fix, {@code app.jwt.secret} defaulted to a value published in this repo,
 * so anybody could mint a valid SUPER_ADMIN JWT against any deployment that relied on the
 * default (verified live: forged token → 200 on {@code GET /api/admins}). The hardening now
 * also removes the "ephemeral development secret": a missing value is fatal in <b>every</b>
 * profile, because a production deployment must never run on throwaway key material.
 */
class SecureSecretInitializerTest {

    /** The value that used to ship in application.properties. */
    static final String SHIPPED_JWT_DEFAULT = "dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw==";
    static final String SHIPPED_SEPAY_DEFAULT = "test-sepay-api-key";

    private GenericApplicationContext context(MockEnvironment environment) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(environment);
        return context;
    }

    private MockEnvironment withValidJwtOnly() {
        return new MockEnvironment()
                .withProperty("app.jwt.secret", "c29tZS1yYW5kb20tbG9va2luZy1qd3Qtc2VjcmV0LWtleQ==");
    }

    @Test
    void rejectsShippedJwtDefault() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", SHIPPED_JWT_DEFAULT)
                .withProperty("sepay.api-key", "a-real-looking-key");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(environment)));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void rejectsSepayDefault() {
        MockEnvironment environment = withValidJwtOnly()
                .withProperty("sepay.api-key", SHIPPED_SEPAY_DEFAULT);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(environment)));
        assertTrue(ex.getMessage().contains("SEPAY_API_KEY"));
    }

    @Test
    void rejectsUnreplacedEnvExamplePlaceholders() {
        // Copying .env.example without editing it must not silently produce a running app
        // whose "secret" is published in the repository.
        MockEnvironment jwtPlaceholder = new MockEnvironment()
                .withProperty("app.jwt.secret", "replace-with-a-random-base64-value-of-at-least-32-bytes")
                .withProperty("sepay.api-key", "a-real-looking-key-value");
        assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(jwtPlaceholder)));

        MockEnvironment sepayPlaceholder = withValidJwtOnly()
                .withProperty("sepay.api-key", "your_api_key");
        assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(sepayPlaceholder)));
    }

    @Test
    void missingSecretsAreFatalInEveryProfile() {
        // Không còn secret sinh tạm cho dev: thiếu key ⇒ app không khởi động được.
        MockEnvironment dev = new MockEnvironment();
        IllegalStateException devEx = assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(dev)));
        assertTrue(devEx.getMessage().contains("JWT_SECRET"));
        assertNull(dev.getProperty("app.jwt.secret"), "must not inject a generated secret");
        assertNull(dev.getProperty("sepay.api-key"), "must not inject a generated secret");

        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        IllegalStateException prodEx = assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(prod)));
        assertTrue(prodEx.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void missingSepayKeyIsFatalEvenWhenJwtSecretIsPresent() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(withValidJwtOnly())));
        assertTrue(ex.getMessage().contains("SEPAY_API_KEY"));
    }

    @Test
    void acceptsExplicitStrongSecrets() {
        // ALLOW_SIMULATION=true để bỏ qua kiểm tra hạ tầng (MySQL/blockchain) — phần secret
        // vẫn được kiểm tra đầy đủ; xem BlockchainSecurityTest cho các ca hạ tầng.
        MockEnvironment environment = withValidJwtOnly()
                .withProperty("sepay.api-key", "sk_live_9f8a7b6c5d4e3f2a1b0c")
                .withProperty("app.allow-simulation", "true")
                .withProperty("app.jwt.secret", "c3Ryb25nLXJhbmRvbS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLXVzZQ==");
        environment.setActiveProfiles("prod");

        assertDoesNotThrow(() -> new SecureSecretInitializer().initialize(context(environment)));
        assertEquals("sk_live_9f8a7b6c5d4e3f2a1b0c", environment.getProperty("sepay.api-key"));
        assertFalse(SecureSecretInitializer.isInsecureSecret(
                "app.jwt.secret", environment.getProperty("app.jwt.secret")));
        assertFalse(SecureSecretInitializer.isInsecureSecret(
                "sepay.api-key", environment.getProperty("sepay.api-key")));
    }
}
