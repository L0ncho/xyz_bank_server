package cl.duoc.xyzbank.coredomain.auth.domain.entities;

import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;

import java.time.Instant;
import java.util.Optional;

public final class RefreshTokenRecord {

    private final Id id;
    private final Id chainId;
    private final Channel channel;
    private final Id ownerId;
    private final String deviceId;
    private final String tokenHash;
    private boolean rotated;
    private boolean revoked;
    private final Instant expiry;
    private final long version;

    private RefreshTokenRecord(
            Id id,
            Id chainId,
            Channel channel,
            Id ownerId,
            String deviceId,
            String tokenHash,
            boolean rotated,
            boolean revoked,
            Instant expiry,
            long version) {
        this.id = id;
        this.chainId = chainId;
        this.channel = channel;
        this.ownerId = ownerId;
        this.deviceId = deviceId;
        this.tokenHash = tokenHash;
        this.rotated = rotated;
        this.revoked = revoked;
        this.expiry = expiry;
        this.version = version;
    }

    public static RefreshTokenRecord create(
            Id id,
            Id chainId,
            Channel channel,
            Id ownerId,
            String deviceId,
            String tokenHash,
            boolean rotated,
            boolean revoked,
            Instant expiry,
            long version) {
        return new RefreshTokenRecord(
                id, chainId, channel, ownerId, deviceId, tokenHash, rotated, revoked, expiry, version);
    }

    public static RefreshTokenRecord issue(
            Id id, Channel channel, Id ownerId, String deviceId, String tokenHash, Instant expiry) {
        return create(id, id, channel, ownerId, deviceId, tokenHash, false, false, expiry, 0L);
    }

    public RefreshTokenRecord rotate(Id newId, String newTokenHash, Instant newExpiry) {
        if (rotated) {
            throw DomainException.conflict("Refresh token was already used; the chain must be revoked");
        }
        rotated = true;
        return create(newId, chainId, channel, ownerId, deviceId, newTokenHash, false, false, newExpiry, 0L);
    }

    public void revoke() {
        revoked = true;
    }

    public Id getId() {
        return id;
    }

    public Id getChainId() {
        return chainId;
    }

    public Channel getChannel() {
        return channel;
    }

    public Id getOwnerId() {
        return ownerId;
    }

    public Optional<String> getDeviceId() {
        return Optional.ofNullable(deviceId);
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public boolean isRotated() {
        return rotated;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getExpiry() {
        return expiry;
    }

    public long getVersion() {
        return version;
    }
}
