package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coreservice.auth.application.dto.PinVerificationRequest;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.VerifyPinUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PinVerificationController {

    private final VerifyPinUseCase verifyPinUseCase;

    public PinVerificationController(VerifyPinUseCase verifyPinUseCase) {
        this.verifyPinUseCase = verifyPinUseCase;
    }

    @PostMapping("/internal/auth/atm/pin-verifications")
    public ResponseEntity<Void> verify(@RequestBody PinVerificationRequest request) {
        Card.PinVerificationResult result = verifyPinUseCase.execute(request.cardNumber(), request.pin());
        HttpStatus status = switch (result) {
            case SUCCESS -> HttpStatus.OK;
            case INCORRECT -> HttpStatus.UNAUTHORIZED;
            case LOCKED -> HttpStatus.LOCKED;
        };
        return ResponseEntity.status(status).build();
    }
}
