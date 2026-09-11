package cl.duoc.xyzbank.coreservice.auth.application.dto;

public record PinVerificationRequest(String cardNumber, String pin) {
}
