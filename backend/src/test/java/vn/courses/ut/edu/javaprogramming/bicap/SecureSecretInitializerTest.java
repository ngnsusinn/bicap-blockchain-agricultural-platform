package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.SecureSecretInitializer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * C-1 / C-3 regression: the repository-shipped default secrets must be unusable.
 *
 * <p>Before the fix, {@code app.jwt.secret} defaulted to a value published in this repo,
 * so anybody could mint a valid SUPER_ADMIN JWT against any deployment that relied on the
 * default (verified live: forged token → 200 on {@code GET /api/admins}). These tests pin
 * the hardening: known values are rejected, blank values become an ephemeral random secret
 * in dev, and blank values are fatal in production.
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
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", "c29tZS1yYW5kb20tbG9va2luZy1qd3Qtc2VjcmV0LWtleQ==")
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

        MockEnvironment sepayPlaceholder = new MockEnvironment()
                .withProperty("app.jwt.secret", "c29tZS1yYW5kb20tbG9va2luZy1qd3Qtc2VjcmV0LWtleQ==")
                .withProperty("sepay.api-key", "your_smtp_password");
        assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(sepayPlaceholder)));
    }

    @Test
    void generatesEphemeralRandomSecretsWhenUnsetInDevelopment() {
        MockEnvironment environment = new MockEnvironment();
        new SecureSecretInitializer().initialize(context(environment));

        String jwt = environment.getProperty("app.jwt.secret");
        String sepay = environment.getProperty("sepay.api-key");
        assertNotNull(jwt);
        assertNotNull(sepay);
        assertNotEquals(SHIPPED_JWT_DEFAULT, jwt);
        assertNotEquals(SHIPPED_SEPAY_DEFAULT, sepay);
        assertTrue(jwt.length() >= 32, "generated JWT key must be at least 32 bytes of material");
        assertFalse(SecureSecretInitializer.isInsecureSecret("app.jwt.secret", jwt));
        assertFalse(SecureSecretInitializer.isInsecureSecret("sepay.api-key", sepay));
    }

    @Test
    void twoRunsGenerateDifferentSecrets() {
        MockEnvironment first = new MockEnvironment();
        MockEnvironment second = new MockEnvironment();
        new SecureSecretInitializer().initialize(context(first));
        new SecureSecretInitializer().initialize(context(second));

        assertNotEquals(first.getProperty("app.jwt.secret"), second.getProperty("app.jwt.secret"));
    }

    @Test
    void failsFastWhenProductionHasNoSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new SecureSecretInitializer().initialize(context(environment)));
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void acceptsExplicitStrongSecrets() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.jwt.secret", "c3Ryb25nLXJhbmRvbS1qd3Qtc2VjcmV0LWZvci1wcm9kdWN0aW9uLXVzZQ==")
                .withProperty("sepay.api-key", "sk_live_9f8a7b6c5d4e3f2a1b0c");
        environment.setActiveProfiles("prod");

        assertDoesNotThrow(() -> new SecureSecretInitializer().initialize(context(environment)));
        assertEquals("sk_live_9f8a7b6c5d4e3f2a1b0c", environment.getProperty("sepay.api-key"));
    }
}
