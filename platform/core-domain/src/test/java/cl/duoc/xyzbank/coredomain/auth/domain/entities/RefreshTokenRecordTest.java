package cl.duoc.xyzbank.coredomain.auth.domain.entities;

import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The RefreshTokenRecord")
class RefreshTokenRecordTest {

    /*
     * Cases:
     * 1. Issuance produces a fresh, unrotated, unrevoked record whose chain id is its own id
     * 2. Successful rotation marks the old record rotated and returns a new record in the same chain
     * 3. Rotating an already-rotated record throws (reuse detected)
     */

    private static final Instant EXPIRY = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NEW_EXPIRY = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    @DisplayName("issuance produces a fresh, unrotated, unrevoked record whose chain id is its own id")
    void issuanceProducesFreshRecord() {
        Id id = Id.generate();
        RefreshTokenRecord record = RefreshTokenRecord.issue(id, Channel.WEB, Id.generate(), null, "hash-1", EXPIRY);

        assertFalse(record.isRotated());
        assertFalse(record.isRevoked());
        assertEquals(id, record.getChainId());
    }

    @Test
    @DisplayName("successful rotation marks the old record rotated and returns a new record in the same chain")
    void successfulRotationMarksOldRecordRotated() {
        RefreshTokenRecord original =
                RefreshTokenRecord.issue(Id.generate(), Channel.WEB, Id.generate(), null, "hash-1", EXPIRY);

        RefreshTokenRecord rotated = original.rotate(Id.generate(), "hash-2", NEW_EXPIRY);

        assertTrue(original.isRotated());
        assertFalse(rotated.isRotated());
        assertFalse(rotated.isRevoked());
        assertEquals(original.getChainId(), rotated.getChainId());
        assertEquals("hash-2", rotated.getTokenHash());
    }

    @Test
    @DisplayName("rotating an already-rotated record throws (reuse detected)")
    void rotatingAlreadyRotatedRecordThrows() {
        RefreshTokenRecord original =
                RefreshTokenRecord.issue(Id.generate(), Channel.WEB, Id.generate(), null, "hash-1", EXPIRY);
        original.rotate(Id.generate(), "hash-2", NEW_EXPIRY);

        assertThrows(DomainException.class, () -> original.rotate(Id.generate(), "hash-3", NEW_EXPIRY));
    }
}
