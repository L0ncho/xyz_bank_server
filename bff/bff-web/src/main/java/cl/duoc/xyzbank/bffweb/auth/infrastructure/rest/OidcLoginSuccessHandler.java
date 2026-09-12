package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.dto.RefreshTokenRequest;
import cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.dto.RefreshTokenResponse;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

/**
 * On a successful OIDC login, mints bff-web's own short-lived JWT for the web channel, sets
 * it (and the first refresh token) as HttpOnly/Secure/SameSite cookies, and obtains that
 * first refresh token from core-service using only the service credential -- no user token
 * exists yet at this point (bff-web-auth spec, channel-auth pre-auth endpoint group).
 */
@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Duration SESSION_COOKIE_TTL = Duration.ofMinutes(15);

    private final JwtCallerContextAdapter tokenAdapter;
    private final RestClient coreServiceClient;
    private final SessionCookieWriter cookieWriter;

    public OidcLoginSuccessHandler(
            JwtCallerContextAdapter tokenAdapter, RestClient coreServiceClient, SessionCookieWriter cookieWriter) {
        this.tokenAdapter = tokenAdapter;
        this.coreServiceClient = coreServiceClient;
        this.cookieWriter = cookieWriter;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String customerId = oidcUser.getSubject();

        String sessionJwt = tokenAdapter.issue(customerId, Channel.WEB, null);
        RefreshTokenResponse refreshTokenResponse = coreServiceClient
                .post()
                .uri("/internal/auth/web/refresh-tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshTokenRequest(customerId, null))
                .retrieve()
                .body(RefreshTokenResponse.class);

        cookieWriter.writeSessionCookies(
                response,
                sessionJwt,
                SESSION_COOKIE_TTL,
                refreshTokenResponse.refreshToken(),
                Duration.between(Instant.now(), refreshTokenResponse.expiry()));
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
