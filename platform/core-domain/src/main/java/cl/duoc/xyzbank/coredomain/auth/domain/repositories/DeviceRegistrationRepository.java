package cl.duoc.xyzbank.coredomain.auth.domain.repositories;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

import java.util.Optional;

public interface DeviceRegistrationRepository {
    void save(DeviceRegistration device);

    Optional<DeviceRegistration> findByDeviceId(Id deviceId);
}
