package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.AuthenticatedCaller;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtAuthenticationException;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtCallerAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";

    private final JwtCallerAuthenticator jwtCallerAuthenticator;
    private final ProblemDetailHttpWriter writer;

    public JwtAuthenticationFilter(JwtCallerAuthenticator jwtCallerAuthenticator, ObjectMapper objectMapper) {
        this.jwtCallerAuthenticator = jwtCallerAuthenticator;
        this.writer = new ProblemDetailHttpWriter(objectMapper);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            writer.write(response, HttpStatus.UNAUTHORIZED, "Bearer token is required");
            return;
        }
        String compactJwt = authorization.substring(BEARER_PREFIX.length()).trim();
        if (compactJwt.isEmpty()) {
            writer.write(response, HttpStatus.UNAUTHORIZED, "Bearer token is required");
            return;
        }
        try {
            AuthenticatedCaller authenticatedCaller = jwtCallerAuthenticator.authenticate(compactJwt);
            SecurityContextHolder.getContext()
                    .setAuthentication(CallerContextAuthentication.authenticated(
                            authenticatedCaller.callerContext(), authenticatedCaller.roles()));
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException exception) {
            writer.write(response, HttpStatus.UNAUTHORIZED, exception.getMessage());
        } catch (CallerIdentityException exception) {
            HttpStatus status = exception.getType() == CallerIdentityException.Type.INVALID
                    ? HttpStatus.UNPROCESSABLE_ENTITY
                    : HttpStatus.FORBIDDEN;
            writer.write(response, status, exception.getMessage());
        }
    }
}
