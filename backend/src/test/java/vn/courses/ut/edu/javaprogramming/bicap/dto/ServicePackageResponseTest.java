package vn.courses.ut.edu.javaprogramming.bicap.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * BICAP — {@code features} must always be exposed as a JSON array string.
 *
 * <p>H2 (local dev default) stores the value in a {@code json} column as a JSON
 * string scalar, so it comes back double-encoded. Clients doing one
 * {@code JSON.parse} then get a String instead of an array and crash.
 */
class ServicePackageResponseTest {

    @Test
    void unwrapsDoubleEncodedValueReturnedByH2() {
        // stored/returned as: "[\"A\",\"B\"]"
        String fromH2 = "\"[\\\"A\\\",\\\"B\\\"]\"";
        assertEquals("[\"A\",\"B\"]", ServicePackageResponse.normalizeFeatures(fromH2));
    }

    @Test
    void keepsAlreadyCorrectJsonArrayUnchanged() {
        assertEquals("[\"A\",\"B\"]", ServicePackageResponse.normalizeFeatures("[\"A\",\"B\"]"));
    }

    @Test
    void doesNotUnwrapTwice() {
        // Idempotent: normalizing an already-normalized value changes nothing.
        String once = ServicePackageResponse.normalizeFeatures("\"[\\\"A\\\",\\\"B\\\"]\"");
        assertEquals(once, ServicePackageResponse.normalizeFeatures(once));
    }

    @Test
    void fallsBackToEmptyArrayForNullOrBlank() {
        assertEquals("[]", ServicePackageResponse.normalizeFeatures(null));
        assertEquals("[]", ServicePackageResponse.normalizeFeatures("   "));
    }

    @Test
    void leavesNonJsonTextAlone() {
        assertEquals("not-json", ServicePackageResponse.normalizeFeatures("not-json"));
    }
}
