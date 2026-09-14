package cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest.dto;

import java.time.Instant;

/** The tokens handed directly to the native mobile client -- no cookie, per design.md Decision 4. */
public record MobileSessionResponse(String sessionToken, String refreshToken, Instant refreshTokenExpiry) {
}
