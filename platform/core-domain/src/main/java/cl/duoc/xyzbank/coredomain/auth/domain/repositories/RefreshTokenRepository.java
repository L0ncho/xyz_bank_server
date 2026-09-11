package cl.duoc.xyzbank.coredomain.auth.domain.repositories;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.RefreshTokenRecord;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(RefreshTokenRecord record);

    Optional<RefreshTokenRecord> findByTokenHash(String tokenHash);

    void revokeChain(Id chainId);
}
