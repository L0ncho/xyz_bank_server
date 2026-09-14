package cl.duoc.xyzbank.coredomain.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.RefreshTokenRecord;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.RefreshTokenRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshTokenRecord> byId = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshTokenRecord record) {
        byId.put(record.getId().getValue(), record);
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return byId.values().stream()
                .filter(record -> record.getTokenHash().equals(tokenHash))
                .findFirst();
    }

    @Override
    public void revokeChain(Id chainId) {
        byId.values().stream()
                .filter(record -> record.getChainId().equals(chainId))
                .forEach(RefreshTokenRecord::revoke);
    }
}
