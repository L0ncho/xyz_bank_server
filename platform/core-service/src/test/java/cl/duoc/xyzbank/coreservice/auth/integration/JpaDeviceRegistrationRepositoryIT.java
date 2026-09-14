package cl.duoc.xyzbank.coreservice.auth.integration;

import cl.duoc.xyzbank.coredomain.accounts.domain.entities.Customer;
import cl.duoc.xyzbank.coredomain.accounts.domain.repositories.CustomerRepository;
import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence.JpaDeviceRegistrationRepository;
import cl.duoc.xyzbank.testsupport.AbstractPostgresIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("The JPA device registration repository")
class JpaDeviceRegistrationRepositoryIT extends AbstractPostgresIT {

    /*
     * Cases:
     * 1. Saves a device registration and finds it by device id
     * 2. Returns empty when no registration matches the device id
     * 3. Revoking a device is reflected on the next find
     */

    @Autowired
    private JpaDeviceRegistrationRepository deviceRegistrationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Id newCustomer() {
        Id customerId = Id.generate();
        customerRepository.save(Customer.create(customerId, "Test Customer", "test.customer@xyzbank.cl"));
        return customerId;
    }

    @Test
    @DisplayName("saves a device registration and finds it by device id")
    void savesADeviceRegistrationAndFindsItByDeviceId() {
        Id deviceId = Id.generate();
        DeviceRegistration device = DeviceRegistration.register(deviceId, newCustomer());

        deviceRegistrationRepository.save(device);
        Optional<DeviceRegistration> found = deviceRegistrationRepository.findByDeviceId(deviceId);

        assertTrue(found.isPresent());
        assertFalse(found.get().isRevoked());
    }

    @Test
    @DisplayName("returns empty when no registration matches the device id")
    void returnsEmptyWhenNoRegistrationMatches() {
        Optional<DeviceRegistration> found = deviceRegistrationRepository.findByDeviceId(Id.generate());

        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("revoking a device is reflected on the next find")
    void revokingDeviceIsReflectedOnNextFind() {
        Id deviceId = Id.generate();
        deviceRegistrationRepository.save(DeviceRegistration.register(deviceId, newCustomer()));
        DeviceRegistration fetched = deviceRegistrationRepository.findByDeviceId(deviceId).orElseThrow();

        fetched.revoke();
        deviceRegistrationRepository.save(fetched);

        DeviceRegistration refetched = deviceRegistrationRepository.findByDeviceId(deviceId).orElseThrow();
        assertTrue(refetched.isRevoked());
    }
}
