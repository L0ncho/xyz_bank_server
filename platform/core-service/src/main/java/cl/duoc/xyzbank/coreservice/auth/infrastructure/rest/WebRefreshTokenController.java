package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenIssuance;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenRequest;
import cl.duoc.xyzbank.coreservice.auth.application.dto.RefreshTokenResponse;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.RotateWebRefreshTokenUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebRefreshTokenController {

    private final RotateWebRefreshTokenUseCase rotateWebRefreshTokenUseCase;

    public WebRefreshTokenController(RotateWebRefreshTokenUseCase rotateWebRefreshTokenUseCase) {
        this.rotateWebRefreshTokenUseCase = rotateWebRefreshTokenUseCase;
    }

    @PostMapping("/internal/auth/web/refresh-tokens")
    public RefreshTokenResponse rotate(@RequestBody RefreshTokenRequest request) {
        RefreshTokenIssuance issuance =
                rotateWebRefreshTokenUseCase.execute(request.customerId(), request.refreshToken());
        return new RefreshTokenResponse(issuance.rawToken(), issuance.expiry());
    }
}
