package cl.duoc.xyzbank.coredomain.auth.domain.entities;

import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

public final class DeviceRegistration {

    private final Id deviceId;
    private final Id customerId;
    private boolean revoked;
    private final long version;

    private DeviceRegistration(Id deviceId, Id customerId, boolean revoked, long version) {
        this.deviceId = deviceId;
        this.customerId = customerId;
        this.revoked = revoked;
        this.version = version;
    }

    public static DeviceRegistration create(Id deviceId, Id customerId, boolean revoked, long version) {
        return new DeviceRegistration(deviceId, customerId, revoked, version);
    }

    public static DeviceRegistration register(Id deviceId, Id customerId) {
        return create(deviceId, customerId, false, 0L);
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

    public long getVersion() {
        return version;
    }
}
