package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.dto;

import java.time.Instant;

public record RefreshTokenResponse(String refreshToken, Instant expiry) {
}
