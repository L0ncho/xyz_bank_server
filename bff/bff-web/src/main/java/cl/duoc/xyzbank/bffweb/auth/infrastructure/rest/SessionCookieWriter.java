package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Writes/clears the session (JWT) and refresh-token cookies bff-web-auth requires: HttpOnly,
 * Secure, and an explicit SameSite attribute, on both login and refresh (bff-web-auth spec,
 * "The session cookie carries HttpOnly, Secure, and SameSite attributes").
 */
@Component
public class SessionCookieWriter {

    static final String SESSION_COOKIE_NAME = "session";
    static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    public void writeSessionCookies(
            HttpServletResponse response, String sessionJwt, Duration sessionTtl, String refreshToken,
            Duration refreshTokenTtl) {
        addCookie(response, SESSION_COOKIE_NAME, sessionJwt, sessionTtl);
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshTokenTtl);
    }

    public void clearSessionCookies(HttpServletResponse response) {
        addCookie(response, SESSION_COOKIE_NAME, "", Duration.ZERO);
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
