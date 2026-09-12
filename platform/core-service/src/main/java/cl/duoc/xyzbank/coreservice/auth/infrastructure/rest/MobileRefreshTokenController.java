package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenIssuance;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenRequest;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenResponse;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.RotateMobileRefreshTokenUseCase;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MobileRefreshTokenController {

    private final RotateMobileRefreshTokenUseCase rotateMobileRefreshTokenUseCase;

    public MobileRefreshTokenController(RotateMobileRefreshTokenUseCase rotateMobileRefreshTokenUseCase) {
        this.rotateMobileRefreshTokenUseCase = rotateMobileRefreshTokenUseCase;
    }

    @PostMapping("/internal/auth/mobile/devices/{deviceId}/refresh-tokens")
    public RefreshTokenResponse rotate(@PathVariable String deviceId, @RequestBody RefreshTokenRequest request) {
        RefreshTokenIssuance issuance = rotateMobileRefreshTokenUseCase.execute(
                request.customerId(), deviceId, request.refreshToken());
        return new RefreshTokenResponse(issuance.customerId(), issuance.rawToken(), issuance.expiry());
    }
}
