package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "chain_id", nullable = false)
    private UUID chainId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private boolean rotated;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private Instant expiry;

    @Version
    @Column(nullable = false)
    private long version;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(
            UUID id,
            UUID chainId,
            Channel channel,
            UUID ownerId,
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

    public UUID getId() {
        return id;
    }

    public UUID getChainId() {
        return chainId;
    }

    public Channel getChannel() {
        return channel;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getDeviceId() {
        return deviceId;
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
