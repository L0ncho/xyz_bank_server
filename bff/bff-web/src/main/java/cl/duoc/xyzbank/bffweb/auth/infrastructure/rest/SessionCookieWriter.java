package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Writes/clears the session (JWT), refresh-token, and CSRF cookies bff-web-auth requires:
 * the session and refresh-token cookies are HttpOnly, Secure, and carry an explicit SameSite
 * attribute ("The session cookie carries HttpOnly, Secure, and SameSite attributes"); the
 * CSRF cookie is deliberately NOT HttpOnly, since client script must read it to echo it back
 * as a header (the double-submit-cookie pattern CsrfFilter validates against).
 */
@Component
public class SessionCookieWriter {

    static final String SESSION_COOKIE_NAME = "session";
    static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void writeSessionCookies(
            HttpServletResponse response, String sessionJwt, Duration sessionTtl, String refreshToken,
            Duration refreshTokenTtl) {
        addCookie(response, SESSION_COOKIE_NAME, sessionJwt, sessionTtl, true);
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshTokenTtl, true);
        addCookie(response, CsrfFilter.COOKIE_NAME, generateCsrfToken(), sessionTtl, false);
    }

    public void clearSessionCookies(HttpServletResponse response) {
        addCookie(response, SESSION_COOKIE_NAME, "", Duration.ZERO, true);
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO, true);
        addCookie(response, CsrfFilter.COOKIE_NAME, "", Duration.ZERO, false);
    }

    private String generateCsrfToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void addCookie(
            HttpServletResponse response, String name, String value, Duration maxAge, boolean httpOnly) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
