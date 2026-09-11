package cl.duoc.xyzbank.coreservice.auth.application.usecases;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.DeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

public class RevokeDeviceUseCase {

    private final DeviceRegistrationRepository deviceRegistrationRepository;

    public RevokeDeviceUseCase(DeviceRegistrationRepository deviceRegistrationRepository) {
        this.deviceRegistrationRepository = deviceRegistrationRepository;
    }

    public void execute(String deviceId) {
        DeviceRegistration device = deviceRegistrationRepository.findByDeviceId(Id.create(deviceId))
                .orElseThrow(() -> DomainException.notFound("Device " + deviceId + " not found"));
        device.revoke();
        deviceRegistrationRepository.save(device);
    }
}
