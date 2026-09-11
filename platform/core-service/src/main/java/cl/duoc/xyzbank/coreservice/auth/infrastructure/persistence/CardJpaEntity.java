package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "cards")
public class CardJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(nullable = false)
    private boolean locked;

    @Version
    @Column(nullable = false)
    private long version;

    protected CardJpaEntity() {
    }

    public CardJpaEntity(
            UUID id, UUID customerId, String pinHash, int consecutiveFailures, boolean locked, long version) {
        this.id = id;
        this.customerId = customerId;
        this.pinHash = pinHash;
        this.consecutiveFailures = consecutiveFailures;
        this.locked = locked;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getPinHash() {
        return pinHash;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public boolean isLocked() {
        return locked;
    }

    public long getVersion() {
        return version;
    }
}
