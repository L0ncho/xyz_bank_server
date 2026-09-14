package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * PIN verification is the one core-service endpoint that carries a raw memorized secret
 * on every call, so it is the sole exception to core-service's plain-HTTP internal API
 * (see channel-transport-security). This filter is registered only for that one path and
 * rejects any request that did not arrive over the TLS connector, before the body (and the
 * PIN it carries) is ever read.
 */
public class PinVerificationTlsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.isSecure()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"detail\":\"This endpoint requires TLS\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
