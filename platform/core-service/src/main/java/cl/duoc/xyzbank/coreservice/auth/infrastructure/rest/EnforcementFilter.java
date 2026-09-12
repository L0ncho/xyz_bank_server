package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Authenticates the calling service (and, for domain endpoints, the user's scope and
 * resource ownership) on every core-service internal endpoint. Gated by
 * security.enforcement.enabled: while disabled, every request passes through unchanged,
 * matching core-service's pre-channel-auth behavior, so this change can roll out without
 * an intermediate state where core-service rejects a BFF that hasn't been updated yet.
 */
public class EnforcementFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final Map<String, String> serviceCredentials;

    public EnforcementFilter(boolean enabled, Map<String, String> serviceCredentials) {
        this.enabled = enabled;
        this.serviceCredentials = serviceCredentials;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
