package cl.duoc.xyzbank.coreservice.auth.integration;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.RefreshTokenRecord;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence.JpaRefreshTokenRepository;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.testsupport.AbstractPostgresIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("The JPA refresh token repository")
class JpaRefreshTokenRepositoryIT extends AbstractPostgresIT {

    /*
     * Cases:
     * 1. Saves a record and finds it by token hash
     * 2. Returns empty when no record matches the token hash
     * 3. Revoking a chain marks every record in that chain revoked
     */

    private static final Instant EXPIRY = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private JpaRefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("saves a record and finds it by token hash")
    void savesARecordAndFindsItByTokenHash() {
        RefreshTokenRecord record =
                RefreshTokenRecord.issue(Id.generate(), Channel.WEB, Id.generate(), null, "hash-abc", EXPIRY);

        refreshTokenRepository.save(record);
        Optional<RefreshTokenRecord> found = refreshTokenRepository.findByTokenHash("hash-abc");

        assertTrue(found.isPresent());
        assertEquals(Channel.WEB, found.get().getChannel());
    }

    @Test
    @DisplayName("returns empty when no record matches the token hash")
    void returnsEmptyWhenNoRecordMatches() {
        Optional<RefreshTokenRecord> found = refreshTokenRepository.findByTokenHash("no-such-hash");

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("revoking a chain marks every record in that chain revoked")
    void revokingChainMarksEveryRecordRevoked() {
        RefreshTokenRecord issued =
                RefreshTokenRecord.issue(Id.generate(), Channel.WEB, Id.generate(), null, "hash-1", EXPIRY);
        refreshTokenRepository.save(issued);
        // Re-fetch before mutating: the in-memory `issued` still carries the pre-insert
        // version, and saving it again unchanged would look like a second insert to
        // Spring Data's isNew() check rather than the update `rotate()` requires.
        RefreshTokenRecord original = refreshTokenRepository.findByTokenHash("hash-1").orElseThrow();
        RefreshTokenRecord rotated = original.rotate(Id.generate(), "hash-2", EXPIRY);
        refreshTokenRepository.save(original);
        refreshTokenRepository.save(rotated);

        refreshTokenRepository.revokeChain(original.getChainId());

        RefreshTokenRecord foundOriginal = refreshTokenRepository.findByTokenHash("hash-1").orElseThrow();
        RefreshTokenRecord foundRotated = refreshTokenRepository.findByTokenHash("hash-2").orElseThrow();
        assertTrue(foundOriginal.isRevoked());
        assertTrue(foundRotated.isRevoked());
    }
}
