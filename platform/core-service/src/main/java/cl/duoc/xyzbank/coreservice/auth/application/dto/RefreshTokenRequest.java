package cl.duoc.xyzbank.coreservice.auth.application.dto;

public record RefreshTokenRequest(String customerId, String refreshToken) {
}
