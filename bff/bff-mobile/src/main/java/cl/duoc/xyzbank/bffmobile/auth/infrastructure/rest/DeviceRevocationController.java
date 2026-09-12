package cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest;

import cl.duoc.xyzbank.bffmobile.shared.infrastructure.adapters.CoreServiceCalls;
import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Forwards a device revocation to core-service. core-service's internal revocation endpoint
 * requires only a service credential and performs no ownership check (proposal.md's pre-auth
 * group), so this controller enforces the self-only invariant here: a device may revoke only
 * itself, never another device, by requiring the path deviceId to match the caller's own
 * X-Device-Id (already proven to match the presented JWT by CallerContextInterceptor).
 */
@RestController
public class DeviceRevocationController {

    private final RestClient coreServiceClient;

    public DeviceRevocationController(RestClient coreServiceClient) {
        this.coreServiceClient = coreServiceClient;
    }

    @PostMapping("/devices/{deviceId}/revocations")
    public ResponseEntity<Void> revoke(@PathVariable String deviceId, @RequestHeader("X-Device-Id") String callerDeviceId) {
        if (!deviceId.equals(callerDeviceId)) {
            throw CallerIdentityException.forbidden("A device may only revoke itself");
        }
        CoreServiceCalls.fetch(() -> coreServiceClient
                .post()
                .uri("/internal/auth/mobile/devices/{deviceId}/revocations", deviceId)
                .retrieve()
                .toBodilessEntity());
        return ResponseEntity.noContent().build();
    }
}
