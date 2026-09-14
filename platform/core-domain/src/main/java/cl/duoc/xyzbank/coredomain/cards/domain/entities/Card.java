package cl.duoc.xyzbank.coredomain.cards.domain.entities;

import cl.duoc.xyzbank.coredomain.cards.domain.services.PinHasher;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

public final class Card {

    private final Id id;
    private final Id customerId;
    private final String pinHash;
    private int consecutiveFailures;
    private boolean locked;
    private final long version;

    private Card(Id id, Id customerId, String pinHash, int consecutiveFailures, boolean locked, long version) {
        this.id = id;
        this.customerId = customerId;
        this.pinHash = pinHash;
        this.consecutiveFailures = consecutiveFailures;
        this.locked = locked;
        this.version = version;
    }

    public static Card create(
            Id id, Id customerId, String pinHash, int consecutiveFailures, boolean locked, long version) {
        return new Card(id, customerId, pinHash, consecutiveFailures, locked, version);
    }

    public Id getId() {
        return id;
    }

    public Id getCustomerId() {
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

    public PinVerificationResult verifyPin(String pin, PinHasher hasher) {
        int maxConsecutiveFailures = 3;
        if (locked) {
            return PinVerificationResult.LOCKED;
        }
        if (hasher.matches(pin, pinHash)) {
            consecutiveFailures = 0;
            return PinVerificationResult.SUCCESS;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= maxConsecutiveFailures) {
            locked = true;
            return PinVerificationResult.LOCKED;
        }
        return PinVerificationResult.INCORRECT;
    }

    public enum PinVerificationResult {
        SUCCESS,
        INCORRECT,
        LOCKED
    }
}
