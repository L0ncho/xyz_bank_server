package cl.duoc.xyzbank.sharedsecurity.callercontext.unit;

import cl.duoc.xyzbank.sharedsecurity.callercontext.OpaqueTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("The OpaqueTokenGenerator")
class OpaqueTokenGeneratorTest {

    /*
     * Cases:
     * 1. Generates a non-blank value
     * 2. Generates a different value on every call
     * 3. Hashes the same value identically every time
     * 4. Hashes different values differently
     * 5. Never returns the raw value as its own hash
     */

    private final OpaqueTokenGenerator generator = new OpaqueTokenGenerator();

    @Test
    @DisplayName("generates a non-blank value")
    void generatesNonBlankValue() {
        assertFalse(generator.generate().isBlank());
    }

    @Test
    @DisplayName("generates a different value on every call")
    void generatesDifferentValueEveryCall() {
        assertNotEquals(generator.generate(), generator.generate());
    }

    @Test
    @DisplayName("hashes the same value identically every time")
    void hashesSameValueIdentically() {
        String value = generator.generate();

        assertEquals(generator.hash(value), generator.hash(value));
    }

    @Test
    @DisplayName("hashes different values differently")
    void hashesDifferentValuesDifferently() {
        assertNotEquals(generator.hash("value-one"), generator.hash("value-two"));
    }

    @Test
    @DisplayName("never returns the raw value as its own hash")
    void neverReturnsRawValueAsHash() {
        String value = generator.generate();

        assertNotEquals(value, generator.hash(value));
    }
}
