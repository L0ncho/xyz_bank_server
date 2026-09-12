package cl.duoc.xyzbank.coreservice.auth.application.usecases;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.RefreshTokenRecord;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.RefreshTokenRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenIssuance;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.OpaqueTokenGenerator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class RotateWebRefreshTokenUseCase {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;
    private final OpaqueTokenGenerator tokenGenerator;
    private final Clock clock;

    public RotateWebRefreshTokenUseCase(RefreshTokenRepository refreshTokenRepository,
            OpaqueTokenGenerator tokenGenerator) {
        this(refreshTokenRepository, tokenGenerator, Clock.systemUTC());
    }

    public RotateWebRefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository, OpaqueTokenGenerator tokenGenerator, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.clock = clock;
    }

    public RefreshTokenIssuance execute(String customerId, String presentedRefreshToken) {
        if (presentedRefreshToken == null) {
            return issue(Id.create(customerId));
        }
        String presentedHash = tokenGenerator.hash(presentedRefreshToken);
        RefreshTokenRecord record = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> DomainException.notFound("Refresh token not found"));
        if (record.isRotated()) {
            refreshTokenRepository.revokeChain(record.getChainId());
            throw DomainException.conflict("Refresh token was already used; the session has been revoked");
        }
        return rotate(record);
    }

    private RefreshTokenIssuance issue(Id customerId) {
        String rawToken = tokenGenerator.generate();
        Instant expiry = clock.instant().plus(REFRESH_TOKEN_TTL);
        RefreshTokenRecord record = RefreshTokenRecord.issue(
                Id.generate(), Channel.WEB, customerId, null, tokenGenerator.hash(rawToken), expiry);
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
