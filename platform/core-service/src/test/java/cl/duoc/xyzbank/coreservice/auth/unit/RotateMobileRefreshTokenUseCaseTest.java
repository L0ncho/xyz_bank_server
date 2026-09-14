package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.unit.InMemoryDeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.auth.unit.InMemoryRefreshTokenRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenIssuance;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.RotateMobileRefreshTokenUseCase;
import cl.duoc.xyzbank.sharedsecurity.callercontext.OpaqueTokenGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The RotateMobileRefreshToken use case")
class RotateMobileRefreshTokenUseCaseTest {

    /*
     * Cases:
     * 1. First-time issuance registers the device and returns a fresh refresh token
     * 2. Successful rotation returns a new refresh token and invalidates the old one
     * 3. Reusing an already-rotated refresh token is rejected and revokes the whole chain
     * 4. Rotation is rejected for a revoked device
     * 5. Rotation is rejected when the presented refresh token belongs to a different device
     */

    private final OpaqueTokenGenerator tokenGenerator = new OpaqueTokenGenerator();
    private final InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
    private final InMemoryDeviceRegistrationRepository deviceRegistrationRepository =
            new InMemoryDeviceRegistrationRepository();
    private final RotateMobileRefreshTokenUseCase useCase =
            new RotateMobileRefreshTokenUseCase(refreshTokenRepository, deviceRegistrationRepository, tokenGenerator);

    @Test
    @DisplayName("first-time issuance registers the device and returns a fresh refresh token")
    void firstTimeIssuanceRegistersDeviceAndReturnsFreshToken() {
        Id deviceId = Id.generate();

        RefreshTokenIssuance issuance = useCase.execute(Id.generate().getValue(), deviceId.getValue(), null);

        assertFalse(issuance.rawToken().isBlank());
        assertTrue(deviceRegistrationRepository.findByDeviceId(deviceId).isPresent());
    }

    @Test
    @DisplayName("successful rotation returns a new token and invalidates the old one")
    void successfulRotationReturnsNewTokenAndInvalidatesOldOne() {
        Id deviceId = Id.generate();
        RefreshTokenIssuance first = useCase.execute(Id.generate().getValue(), deviceId.getValue(), null);

        RefreshTokenIssuance rotated = useCase.execute(null, deviceId.getValue(), first.rawToken());

        assertNotEquals(first.rawToken(), rotated.rawToken());
        assertTrue(refreshTokenRepository.findByTokenHash(tokenGenerator.hash(first.rawToken()))
                .orElseThrow()
                .isRotated());
    }

    @Test
    @DisplayName("reusing an already-rotated refresh token is rejected and revokes the whole chain")
    void reusingAlreadyRotatedTokenIsRejectedAndRevokesChain() {
        Id deviceId = Id.generate();
        RefreshTokenIssuance first = useCase.execute(Id.generate().getValue(), deviceId.getValue(), null);
        useCase.execute(null, deviceId.getValue(), first.rawToken());

        assertThrows(DomainException.class, () -> useCase.execute(null, deviceId.getValue(), first.rawToken()));

        assertTrue(refreshTokenRepository.findByTokenHash(tokenGenerator.hash(first.rawToken()))
                .orElseThrow()
                .isRevoked());
    }

    @Test
    @DisplayName("rotation is rejected for a revoked device")
    void rotationIsRejectedForRevokedDevice() {
        Id deviceId = Id.generate();
        RefreshTokenIssuance first = useCase.execute(Id.generate().getValue(), deviceId.getValue(), null);
        DeviceRegistration device = deviceRegistrationRepository.findByDeviceId(deviceId).orElseThrow();
        device.revoke();
        deviceRegistrationRepository.save(device);

        assertThrows(DomainException.class, () -> useCase.execute(null, deviceId.getValue(), first.rawToken()));
    }

    @Test
    @DisplayName("rotation is rejected when the presented refresh token belongs to a different device")
    void rotationIsRejectedWhenTheTokenBelongsToADifferentDevice() {
        Id ownDeviceId = Id.generate();
        Id otherDeviceId = Id.generate();
        RefreshTokenIssuance issuedForOtherDevice = useCase.execute(Id.generate().getValue(), otherDeviceId.getValue(), null);
        useCase.execute(Id.generate().getValue(), ownDeviceId.getValue(), null);

        DomainException exception = assertThrows(
                DomainException.class,
                () -> useCase.execute(null, ownDeviceId.getValue(), issuedForOtherDevice.rawToken()));

        assertTrue(exception.getType() == DomainException.Type.NOT_FOUND);
        assertFalse(refreshTokenRepository.findByTokenHash(tokenGenerator.hash(issuedForOtherDevice.rawToken()))
                .orElseThrow()
                .isRotated(), "the other device's token must not be rotated by this mismatched attempt");
    }
}
