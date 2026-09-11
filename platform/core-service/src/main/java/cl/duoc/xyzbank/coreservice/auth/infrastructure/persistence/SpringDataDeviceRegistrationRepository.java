package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDeviceRegistrationRepository extends JpaRepository<DeviceRegistrationJpaEntity, String> {
}
