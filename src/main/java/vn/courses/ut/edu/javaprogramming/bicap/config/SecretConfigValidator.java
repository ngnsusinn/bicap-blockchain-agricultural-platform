package vn.courses.ut.edu.javaprogramming.bicap.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fail-fast validation of deploy-time secrets (C-1, C-3):
 * the app refuses to boot when JWT_SECRET or SEPAY_API_KEY are missing or set to a
 * known placeholder/weak value. This removes the "known default secret" backdoor —
 * a deployment that forgets to configure them stops instead of running insecure.
 */
@Component
public class SecretConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(SecretConfigValidator.class);

    /** Secrets that are publicly shipped and must never be accepted at runtime. */
    private static final String[] FORBIDDEN_JWT_SECRETS = {
            "dGVzdF9zdXBlcl9zZWNyZXRfa2V5X3doaWNoX2lzX2F0X2xlYXN0XzMyX2J5dGVzX2xvbmc=",
            "defaultSecretKeyWhichShouldBeAtLeast32BytesLongForHS256Algorithm"
    };

    private static final String[] FORBIDDEN_SEPAY_KEYS = {
            "YOUR_SEPAY_API_KEY"
    };

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${sepay.api-key:}")
    private String sepayApiKey;

    @Value("${blockchain.mode:mock}")
    private String blockchainMode;

    @Value("${blockchain.private-key:}")
    private String blockchainPrivateKey;

    @PostConstruct
    void validate() {
        if (isBlank(jwtSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is not configured. Set the JWT_SECRET environment variable "
                            + "(a random Base64 value of at least 32 bytes) before starting the application.");
        }
        if (containsForbidden(jwtSecret, FORBIDDEN_JWT_SECRETS)) {
            throw new IllegalStateException(
                    "JWT_SECRET is set to a known, publicly-shipped default value. "
                            + "Generate a new random secret before starting the application.");
        }
        if (decodedKeyLength(jwtSecret) < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET decodes to fewer than 32 bytes — it must be at least 32 bytes of key material.");
        }

        if (isBlank(sepayApiKey)) {
            throw new IllegalStateException(
                    "SEPAY_API_KEY is not configured. Set the SEPAY_API_KEY environment variable "
                            + "before starting the application.");
        }
        if (containsForbidden(sepayApiKey, FORBIDDEN_SEPAY_KEYS)) {
            throw new IllegalStateException(
                    "SEPAY_API_KEY is set to the placeholder value '" + sepayApiKey
                            + "'. Configure the real Sepay API key before starting the application.");
        }

        // BICAP-74/81: live VeChainThor mode is useless (and misleading) without a signer key.
        if ("live".equalsIgnoreCase(blockchainMode) && isBlank(blockchainPrivateKey)) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_PRIVATE_KEY is required when BLOCKCHAIN_MODE=live. "
                            + "Set it to the hex private key of the platform signer wallet, "
                            + "or run with BLOCKCHAIN_MODE=mock.");
        }

        log.info("Deploy-time secrets validated (JWT_SECRET, SEPAY_API_KEY, blockchain mode={})", blockchainMode);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean containsForbidden(String value, String[] forbidden) {
        for (String f : forbidden) {
            if (value.equalsIgnoreCase(f)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the decoded byte length of the key, tolerating Base64, Base64URL or raw text. */
    private int decodedKeyLength(String value) {
        try {
            return java.util.Base64.getDecoder().decode(value).length;
        } catch (IllegalArgumentException e) {
            try {
                return java.util.Base64.getUrlDecoder().decode(value).length;
            } catch (IllegalArgumentException ex) {
                return value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            }
        }
    }
}
