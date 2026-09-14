package cl.duoc.xyzbank.sharedsecurity.callercontext.unit;

import cl.duoc.xyzbank.sharedsecurity.callercontext.PinHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The PinHasher")
class PinHasherTest {

    /*
     * Cases:
     * 1. Hash of a PIN never equals the raw PIN
     * 2. Verifies the correct PIN against its own hash
     * 3. Rejects an incorrect PIN against a hash
     * 4. Hashing the same PIN twice produces different hashes (salted)
     */

    private final PinHasher hasher = new PinHasher();

    @Test
    @DisplayName("hash of a pin never equals the raw pin")
    void hashNeverEqualsRawPin() {
        assertNotEquals("1234", hasher.hash("1234"));
    }

    @Test
    @DisplayName("verifies the correct pin against its own hash")
    void verifiesCorrectPin() {
        String hash = hasher.hash("1234");

        assertTrue(hasher.matches("1234", hash));
    }

    @Test
    @DisplayName("rejects an incorrect pin against a hash")
    void rejectsIncorrectPin() {
        String hash = hasher.hash("1234");

        assertFalse(hasher.matches("9999", hash));
    }

    @Test
    @DisplayName("hashing the same pin twice produces different hashes")
    void hashingSamePinTwiceProducesDifferentHashes() {
        assertNotEquals(hasher.hash("1234"), hasher.hash("1234"));
    }
}
