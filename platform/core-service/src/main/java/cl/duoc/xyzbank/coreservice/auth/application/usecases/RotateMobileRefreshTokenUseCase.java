package cl.duoc.xyzbank.coreservice.auth.application.usecases;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.domain.entities.RefreshTokenRecord;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.DeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.RefreshTokenRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenIssuance;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.OpaqueTokenGenerator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RotateMobileRefreshTokenUseCase {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(180);

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceRegistrationRepository deviceRegistrationRepository;
    private final OpaqueTokenGenerator tokenGenerator;
    private final Clock clock;

    public RotateMobileRefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            DeviceRegistrationRepository deviceRegistrationRepository,
            OpaqueTokenGenerator tokenGenerator) {
        this(refreshTokenRepository, deviceRegistrationRepository, tokenGenerator, Clock.systemUTC());
    }

    public RotateMobileRefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            DeviceRegistrationRepository deviceRegistrationRepository,
            OpaqueTokenGenerator tokenGenerator,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.deviceRegistrationRepository = deviceRegistrationRepository;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    public RefreshTokenIssuance execute(String customerId, String deviceId, String presentedRefreshToken) {
        Id device = Id.create(deviceId);
        if (presentedRefreshToken == null) {
            return issue(Id.create(customerId), device);
        }
        DeviceRegistration registration = deviceRegistrationRepository.findByDeviceId(device)
                .orElseThrow(() -> DomainException.notFound("Device not registered"));
        registration.assertActive();

        String presentedHash = tokenGenerator.hash(presentedRefreshToken);
        RefreshTokenRecord record = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> DomainException.notFound("Refresh token not found"));
        if (record.isRotated()) {
            refreshTokenRepository.revokeChain(record.getChainId());
            throw DomainException.conflict("Refresh token was already used; the session has been revoked");
        }
        return rotate(record);
    }

    private RefreshTokenIssuance issue(Id customerId, Id deviceId) {
        DeviceRegistration registration = deviceRegistrationRepository.findByDeviceId(deviceId)
                .orElseGet(() -> DeviceRegistration.register(deviceId, customerId));
        registration.assertActive();
        deviceRegistrationRepository.save(registration);

        String rawToken = tokenGenerator.generate();
        Instant expiry = clock.instant().plus(REFRESH_TOKEN_TTL);
        RefreshTokenRecord record = RefreshTokenRecord.issue(
                Id.generate(), Channel.MOBILE, customerId, deviceId.getValue(), tokenGenerator.hash(rawToken),
                expiry);
        refreshTokenRepository.save(record);
        return new RefreshTokenIssuance(customerId.getValue(), rawToken, expiry);
    }

    private RefreshTokenIssuance rotate(RefreshTokenRecord record) {
        String rawToken = tokenGenerator.generate();
        Instant expiry = clock.instant().plus(REFRESH_TOKEN_TTL);
        RefreshTokenRecord rotated = record.rotate(Id.generate(), tokenGenerator.hash(rawToken), expiry);
        refreshTokenRepository.save(record);
        refreshTokenRepository.save(rotated);
        return new RefreshTokenIssuance(record.getOwnerId().getValue(), rawToken, expiry);
    }
}
