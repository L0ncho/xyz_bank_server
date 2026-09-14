package cl.duoc.xyzbank.coredomain.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.DeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDeviceRegistrationRepository implements DeviceRegistrationRepository {

    private final Map<String, DeviceRegistration> devices = new ConcurrentHashMap<>();

    @Override
    public void save(DeviceRegistration device) {
        devices.put(device.getDeviceId().getValue(), device);
    }

    @Override
    public Optional<DeviceRegistration> findByDeviceId(Id deviceId) {
        return Optional.ofNullable(devices.get(deviceId.getValue()));
    }
}
