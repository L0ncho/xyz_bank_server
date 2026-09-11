package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.RefreshTokenRecord;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.RefreshTokenRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository jpaRepository;

    public JpaRefreshTokenRepository(SpringDataRefreshTokenRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(RefreshTokenRecord record) {
        try {
            jpaRepository.saveAndFlush(toEntity(record));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw DomainException.conflict("Refresh token record was updated concurrently");
        }
    }

    @Override
    public Optional<RefreshTokenRecord> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revokeChain(Id chainId) {
        jpaRepository.revokeByChainId(UUID.fromString(chainId.getValue()));
    }

    private RefreshTokenJpaEntity toEntity(RefreshTokenRecord record) {
        return new RefreshTokenJpaEntity(
                UUID.fromString(record.getId().getValue()),
                UUID.fromString(record.getChainId().getValue()),
                record.getChannel(),
                UUID.fromString(record.getOwnerId().getValue()),
                record.getDeviceId().orElse(null),
                record.getTokenHash(),
                record.isRotated(),
                record.isRevoked(),
                record.getExpiry(),
                record.getVersion());
    }

    private RefreshTokenRecord toDomain(RefreshTokenJpaEntity entity) {
        return RefreshTokenRecord.create(
                Id.create(entity.getId().toString()),
                Id.create(entity.getChainId().toString()),
                entity.getChannel(),
                Id.create(entity.getOwnerId().toString()),
                entity.getDeviceId(),
                entity.getTokenHash(),
                entity.isRotated(),
                entity.isRevoked(),
                entity.getExpiry(),
                entity.getVersion());
    }
}
