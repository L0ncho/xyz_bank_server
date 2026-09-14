package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.dto;

public record RefreshTokenRequest(String customerId, String refreshToken) {
}
