package cl.duoc.xyzbank.coredomain.auth.domain.entities;

import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The DeviceRegistration")
class DeviceRegistrationTest {

    /*
     * Cases:
     * 1. Registering a new device is not revoked and does not reject use
     * 2. Revoking a device marks it revoked
     * 3. A revoked device rejects further use
     */

    @Test
    @DisplayName("registering a new device is not revoked and does not reject use")
    void registeringNewDeviceIsNotRevoked() {
        DeviceRegistration device = DeviceRegistration.register(Id.generate(), Id.generate());

        assertFalse(device.isRevoked());
        assertDoesNotThrow(device::assertActive);
    }

    @Test
    @DisplayName("revoking a device marks it revoked")
    void revokingDeviceMarksItRevoked() {
        DeviceRegistration device = DeviceRegistration.register(Id.generate(), Id.generate());

        device.revoke();

        assertTrue(device.isRevoked());
    }

    @Test
    @DisplayName("a revoked device rejects further use")
    void revokedDeviceRejectsFurtherUse() {
        DeviceRegistration device = DeviceRegistration.register(Id.generate(), Id.generate());
        device.revoke();

        assertThrows(DomainException.class, device::assertActive);
    }
}
