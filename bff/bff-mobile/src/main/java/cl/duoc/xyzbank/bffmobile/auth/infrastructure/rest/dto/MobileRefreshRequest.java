package cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest.dto;

public record MobileRefreshRequest(String deviceId, String refreshToken) {
}
