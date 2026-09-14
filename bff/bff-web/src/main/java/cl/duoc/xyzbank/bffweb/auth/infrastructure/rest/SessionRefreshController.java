package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.dto.RefreshTokenRequest;
import cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.dto.RefreshTokenResponse;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;

/**
 * Rotates the customer's session: exchanges the refresh_token cookie for a new one via
 * core-service and mints a fresh session JWT (bff-web-auth spec, "the refresh token rotates
 * on every use and detects reuse"). Reachable without a valid session JWT -- that is the
 * point of a refresh endpoint -- but does require the refresh_token cookie.
 */
@RestController
public class SessionRefreshController {

    private static final Duration SESSION_COOKIE_TTL = Duration.ofMinutes(15);

    private final JwtCallerContextAdapter tokenAdapter;
    private final RestClient coreServiceClient;
    private final SessionCookieWriter cookieWriter;

    public SessionRefreshController(
            JwtCallerContextAdapter tokenAdapter, RestClient coreServiceClient, SessionCookieWriter cookieWriter) {
        this.tokenAdapter = tokenAdapter;
        this.coreServiceClient = coreServiceClient;
        this.cookieWriter = cookieWriter;
    }

    @PostMapping("/session/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            cookieWriter.clearSessionCookies(response);
            return ResponseEntity.status(401).build();
        }

        RefreshTokenResponse refreshTokenResponse;
        try {
            refreshTokenResponse = coreServiceClient
                    .post()
                    .uri("/internal/auth/web/refresh-tokens")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RefreshTokenRequest(null, refreshToken))
                    .retrieve()
                    .body(RefreshTokenResponse.class);
        } catch (RestClientResponseException exception) {
            cookieWriter.clearSessionCookies(response);
            return ResponseEntity.status(401).build();
        }

        String sessionJwt = tokenAdapter.issue(refreshTokenResponse.customerId(), Channel.WEB, null);
        cookieWriter.writeSessionCookies(
                response,
                sessionJwt,
                SESSION_COOKIE_TTL,
                refreshTokenResponse.refreshToken(),
                Duration.between(Instant.now(), refreshTokenResponse.expiry()));
        return ResponseEntity.noContent().build();
    }
}
