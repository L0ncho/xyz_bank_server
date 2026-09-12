package cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest;

import cl.duoc.xyzbank.bffmobile.auth.config.DeviceCapturingAuthorizationRequestResolver;
import cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest.dto.MobileSessionResponse;
import cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest.dto.RefreshTokenRequest;
import cl.duoc.xyzbank.bffmobile.auth.infrastructure.rest.dto.RefreshTokenResponse;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

/**
 * On a successful OIDC login, mints bff-mobile's own device-bound JWT for the mobile channel
 * and registers the device with core-service, obtaining the first refresh token -- both
 * handed directly to the native client in the response body, never a cookie (design.md
 * Decision 4). The device identifier travels via DeviceCapturingAuthorizationRequestResolver,
 * captured from the login-initiation request before the OIDC round trip began.
 */
@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtCallerContextAdapter tokenAdapter;
    private final RestClient coreServiceClient;
    private final ObjectMapper objectMapper;

    public OidcLoginSuccessHandler(
            JwtCallerContextAdapter tokenAdapter, RestClient coreServiceClient, ObjectMapper objectMapper) {
        this.tokenAdapter = tokenAdapter;
        this.coreServiceClient = coreServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        Object deviceIdAttribute =
                request.getSession().getAttribute(DeviceCapturingAuthorizationRequestResolver.DEVICE_ID_SESSION_ATTRIBUTE);
        if (deviceIdAttribute == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String deviceId = deviceIdAttribute.toString();

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String customerId = oidcUser.getSubject();

        String sessionJwt = tokenAdapter.issue(customerId, Channel.MOBILE, deviceId);
        RefreshTokenResponse refreshTokenResponse = coreServiceClient
                .post()
                .uri("/internal/auth/mobile/devices/{deviceId}/refresh-tokens", deviceId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshTokenRequest(customerId, null))
                .retrieve()
                .body(RefreshTokenResponse.class);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                new MobileSessionResponse(
                        sessionJwt, refreshTokenResponse.refreshToken(), refreshTokenResponse.expiry()));
    }
}
