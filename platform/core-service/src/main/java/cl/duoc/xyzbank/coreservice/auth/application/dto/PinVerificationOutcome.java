package cl.duoc.xyzbank.coreservice.auth.application.dto;

public record PinVerificationOutcome(Result result, String customerId) {

    public enum Result {
        SUCCESS,
        INCORRECT,
        LOCKED
    }
}
