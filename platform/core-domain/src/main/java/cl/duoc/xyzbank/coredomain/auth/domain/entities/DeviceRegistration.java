package cl.duoc.xyzbank.coredomain.auth.domain.entities;

import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

public final class DeviceRegistration {

    private final Id deviceId;
    private final Id customerId;
    private boolean revoked;

    private DeviceRegistration(Id deviceId, Id customerId, boolean revoked) {
        this.deviceId = deviceId;
        this.customerId = customerId;
        this.revoked = revoked;
    }

    public static DeviceRegistration create(Id deviceId, Id customerId, boolean revoked) {
        return new DeviceRegistration(deviceId, customerId, revoked);
    }

    public static DeviceRegistration register(Id deviceId, Id customerId) {
        return create(deviceId, customerId, false);
    }

    public void revoke() {
        revoked = true;
    }

    public void assertActive() {
        if (revoked) {
            throw DomainException.conflict("Device is revoked");
        }
    }

    public Id getDeviceId() {
        return deviceId;
    }

    public Id getCustomerId() {
        return customerId;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
