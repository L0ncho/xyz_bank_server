package cl.duoc.xyzbank.coreservice.auth.application.dto;

import java.time.Instant;

public record RefreshTokenResponse(String customerId, String refreshToken, Instant expiry) {
}
