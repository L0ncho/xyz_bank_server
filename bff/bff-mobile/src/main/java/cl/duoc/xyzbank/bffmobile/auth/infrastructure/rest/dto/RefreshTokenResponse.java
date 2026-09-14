package cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest.dto;

import java.time.Instant;

public record RefreshTokenResponse(String customerId, String refreshToken, Instant expiry) {
}
