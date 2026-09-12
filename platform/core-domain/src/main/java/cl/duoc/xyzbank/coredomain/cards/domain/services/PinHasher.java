package cl.duoc.xyzbank.coredomain.cards.domain.services;

/**
 * Domain port for verifying a submitted PIN against its stored hash. The hashing
 * algorithm is an infrastructure concern; only the concrete adapter (e.g. BCrypt-backed)
 * lives outside the domain.
 */
@FunctionalInterface
public interface PinHasher {
    boolean matches(String pin, String hash);
}
