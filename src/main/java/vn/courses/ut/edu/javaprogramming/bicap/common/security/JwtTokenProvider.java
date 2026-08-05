package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private int jwtExpirationMs;

    @Value("${app.jwt.retailer-access-expiration-ms:900000}")
    private long retailerAccessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${app.jwt.verification-expiration-ms:86400000}")
    private long verificationExpirationMs;

    private SecretKey key() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            try {
                keyBytes = Decoders.BASE64URL.decode(jwtSecret);
            } catch (Exception ex) {
                keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTypedToken(userPrincipal.getUsername(), "access", jwtExpirationMs);
    }

    public String generateRetailerAccessToken(UserDetails userPrincipal) {
        return generateTypedToken(userPrincipal.getUsername(), "access", retailerAccessExpirationMs);
    }

    public String generateRefreshToken(UserDetails userPrincipal) {
        return generateTypedToken(userPrincipal.getUsername(), "refresh", refreshExpirationMs);
    }

    public String generateEmailVerificationToken(UserDetails userPrincipal) {
        return generateTypedToken(userPrincipal.getUsername(), "email_verification", verificationExpirationMs);
    }

    private String generateTypedToken(String subject, String type, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("type", type))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    public boolean isTokenType(String token, String expectedType) {
        try {
            String type = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("type", String.class);
            return expectedType.equals(type);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            String type = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(authToken)
                    .getPayload()
                    .get("type", String.class);
            // Tokens issued before typed JWTs were introduced have no type claim.
            // Keep those existing sessions valid while rejecting typed refresh/
            // verification tokens at the authentication filter.
            return type == null || "access".equals(type);
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }
}
