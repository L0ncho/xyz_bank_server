package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.unit.InMemoryRefreshTokenRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenIssuance;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.RotateWebRefreshTokenUseCase;
import cl.duoc.xyzbank.sharedsecurity.callercontext.OpaqueTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The RotateWebRefreshToken use case")
class RotateWebRefreshTokenUseCaseTest {

    /*
     * Cases:
     * 1. First-time issuance (no prior token) returns a fresh refresh token
     * 2. Successful rotation returns a new refresh token and invalidates the old one
     * 3. Reusing an already-rotated refresh token is rejected and revokes the whole chain
     */

    private final OpaqueTokenGenerator tokenGenerator = new OpaqueTokenGenerator();
    private final InMemoryRefreshTokenRepository repository = new InMemoryRefreshTokenRepository();
    private final RotateWebRefreshTokenUseCase useCase =
            new RotateWebRefreshTokenUseCase(repository, tokenGenerator);

    @Test
    @DisplayName("first-time issuance returns a fresh refresh token")
    void firstTimeIssuanceReturnsFreshRefreshToken() {
        RefreshTokenIssuance issuance = useCase.execute(Id.generate().getValue(), null);

        assertFalse(issuance.rawToken().isBlank());
        assertTrue(repository.findByTokenHash(tokenGenerator.hash(issuance.rawToken())).isPresent());
    }

    @Test
    @DisplayName("successful rotation returns a new token and invalidates the old one")
    void successfulRotationReturnsNewTokenAndInvalidatesOldOne() {
        RefreshTokenIssuance first = useCase.execute(Id.generate().getValue(), null);

        RefreshTokenIssuance rotated = useCase.execute(null, first.rawToken());

        assertNotEquals(first.rawToken(), rotated.rawToken());
        assertTrue(repository.findByTokenHash(tokenGenerator.hash(first.rawToken())).orElseThrow().isRotated());
        assertTrue(repository.findByTokenHash(tokenGenerator.hash(rotated.rawToken())).isPresent());
    }

    @Test
    @DisplayName("reusing an already-rotated refresh token is rejected and revokes the whole chain")
    void reusingAlreadyRotatedTokenIsRejectedAndRevokesChain() {
        RefreshTokenIssuance first = useCase.execute(Id.generate().getValue(), null);
        RefreshTokenIssuance second = useCase.execute(null, first.rawToken());

        assertThrows(DomainException.class, () -> useCase.execute(null, first.rawToken()));

        assertTrue(repository.findByTokenHash(tokenGenerator.hash(first.rawToken())).orElseThrow().isRevoked());
        assertTrue(repository.findByTokenHash(tokenGenerator.hash(second.rawToken())).orElseThrow().isRevoked());
    }
}
