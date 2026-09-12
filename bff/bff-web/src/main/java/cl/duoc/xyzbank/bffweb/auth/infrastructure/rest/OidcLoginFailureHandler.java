package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Placeholder wired for task 9.1's reachability check; task 9.4 replaces the body with the
 * real rejection path: no cookie is set and no refresh token is requested.
 */
@Component
public class OidcLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
