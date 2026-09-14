package cl.duoc.xyzbank.coreservice.auth.application.dto;

import java.time.Instant;

public record RefreshTokenIssuance(String customerId, String rawToken, Instant expiry) {
}
