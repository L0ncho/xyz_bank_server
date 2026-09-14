package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.DeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaDeviceRegistrationRepository implements DeviceRegistrationRepository {

    private final SpringDataDeviceRegistrationRepository jpaRepository;

    public JpaDeviceRegistrationRepository(SpringDataDeviceRegistrationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(DeviceRegistration device) {
        try {
            jpaRepository.saveAndFlush(toEntity(device));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw DomainException.conflict("Device registration was updated concurrently");
        }
    }

    @Override
    public Optional<DeviceRegistration> findByDeviceId(Id deviceId) {
        return jpaRepository.findById(deviceId.getValue()).map(this::toDomain);
    }

    private DeviceRegistrationJpaEntity toEntity(DeviceRegistration device) {
        return new DeviceRegistrationJpaEntity(
                device.getDeviceId().getValue(),
                UUID.fromString(device.getCustomerId().getValue()),
                device.isRevoked(),
                device.getVersion());
    }

    private DeviceRegistration toDomain(DeviceRegistrationJpaEntity entity) {
        return DeviceRegistration.create(
                Id.create(entity.getDeviceId()),
                Id.create(entity.getCustomerId().toString()),
                entity.isRevoked(),
                entity.getVersion());
    }
}
