package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "device_registrations")
public class DeviceRegistrationJpaEntity {

    @Id
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private boolean revoked;

    @Version
    @Column(nullable = false)
    private long version;

    protected DeviceRegistrationJpaEntity() {
    }

    public DeviceRegistrationJpaEntity(String deviceId, UUID customerId, boolean revoked, long version) {
        this.deviceId = deviceId;
        this.customerId = customerId;
        this.revoked = revoked;
        this.version = version;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public long getVersion() {
        return version;
    }
}
