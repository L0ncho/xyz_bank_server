package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import cl.duoc.xyzbank.coreservice.auth.application.usecases.RevokeDeviceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceRevocationController {

    private final RevokeDeviceUseCase revokeDeviceUseCase;

    public DeviceRevocationController(RevokeDeviceUseCase revokeDeviceUseCase) {
        this.revokeDeviceUseCase = revokeDeviceUseCase;
    }

    @PostMapping("/internal/auth/mobile/devices/{deviceId}/revocations")
    public ResponseEntity<Void> revoke(@PathVariable String deviceId) {
        revokeDeviceUseCase.execute(deviceId);
        return ResponseEntity.noContent().build();
    }
}
