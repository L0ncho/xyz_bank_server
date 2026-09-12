package cl.duoc.xyzbank.bffatm.auth.infrastructure.rest.dto;

public record PinVerificationRequest(String cardNumber, String pin) {
}
