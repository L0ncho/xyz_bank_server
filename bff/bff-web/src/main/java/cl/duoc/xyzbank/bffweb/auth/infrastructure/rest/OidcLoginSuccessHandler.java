package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Placeholder wired for task 9.1's reachability check; task 9.3 replaces the body with the
 * real handshake: mint the web-channel JWT, set it as an HttpOnly/Secure/SameSite cookie, and
 * obtain the first refresh token from core-service.
 */
@Component
public class OidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
