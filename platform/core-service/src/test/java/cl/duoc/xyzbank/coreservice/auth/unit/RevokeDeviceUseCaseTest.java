package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.unit.InMemoryDeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.RevokeDeviceUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The RevokeDevice use case")
class RevokeDeviceUseCaseTest {

    /*
     * Cases:
     * 1. Revokes the targeted device
     * 2. Does not affect another device registered to the same customer
     */

    private final InMemoryDeviceRegistrationRepository repository = new InMemoryDeviceRegistrationRepository();
    private final RevokeDeviceUseCase useCase = new RevokeDeviceUseCase(repository);

    @Test
    @DisplayName("revokes the targeted device")
    void revokesTargetedDevice() {
        Id customerId = Id.generate();
        Id deviceId = Id.generate();
        repository.save(DeviceRegistration.register(deviceId, customerId));

        useCase.execute(deviceId.getValue());

        assertTrue(repository.findByDeviceId(deviceId).orElseThrow().isRevoked());
    }

    @Test
    @DisplayName("does not affect another device registered to the same customer")
    void doesNotAffectAnotherDeviceForSameCustomer() {
        Id customerId = Id.generate();
        Id deviceOne = Id.generate();
        Id deviceTwo = Id.generate();
        repository.save(DeviceRegistration.register(deviceOne, customerId));
        repository.save(DeviceRegistration.register(deviceTwo, customerId));

        useCase.execute(deviceOne.getValue());

        assertFalse(repository.findByDeviceId(deviceTwo).orElseThrow().isRevoked());
    }
}
