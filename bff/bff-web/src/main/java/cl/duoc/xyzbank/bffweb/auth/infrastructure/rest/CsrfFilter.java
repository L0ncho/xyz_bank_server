package cl.duoc.xyzbank.bffweb.auth.infrastructure.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Rejects any cookie-authenticated, state-changing request that lacks a CSRF token matching
 * its own cookie (bff-web-auth spec). Safe (read-only) requests never need one. Uses the
 * double-submit-cookie pattern: the token cookie is deliberately not HttpOnly (client script
 * must read it to echo it back), matching the common XSRF-TOKEN / X-XSRF-TOKEN convention.
 */
@Component
public class CsrfFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "XSRF-TOKEN";
    static final String HEADER_NAME = "X-XSRF-TOKEN";

    private static final Set<String> SAFE_METHODS =
            Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name(), HttpMethod.TRACE.name());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String cookieToken = cookieValue(request);
        String headerToken = request.getHeader(HEADER_NAME);
        if (cookieToken == null || headerToken == null || !cookieToken.equals(headerToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write("{\"detail\":\"A valid CSRF token is required\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String cookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
