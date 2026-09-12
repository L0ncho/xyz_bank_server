package cl.duoc.xyzbank.bffatm.shared.infrastructure.rest;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerContext;
import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Resolves the caller's identity from two independent factors, both required on every
 * protected request: the ATM terminal's mTLS client certificate (identifies the physical
 * terminal, validated by the TLS connector itself before this interceptor ever runs) and the
 * session JWT issued after a successful PIN verification (identifies the customer, bound to a
 * terminal id at issuance). A session presented from a different terminal than the one it was
 * bound to is rejected -- a stolen session token must not authenticate requests from another
 * terminal even though every ATM shares the same TLS trust chain.
 */
@Component
public class CallerContextInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_TOKEN_MDC_KEY = "userToken";

    private final JwtCallerContextAdapter tokenAdapter;

    public CallerContextInterceptor(JwtCallerContextAdapter tokenAdapter) {
        this.tokenAdapter = tokenAdapter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String terminalId = ClientCertificateTerminalResolver.resolveTerminalId(request)
                .orElseThrow(() -> CallerIdentityException.invalid("Missing terminal certificate"));

        String bearerToken = extractBearerToken(request);
        if (bearerToken == null) {
            throw CallerIdentityException.invalid("Missing bearer token");
        }

        CallerContext callerContext = tokenAdapter.resolve(bearerToken);
        if (callerContext.channel() != Channel.ATM) {
            throw CallerIdentityException.forbidden("This endpoint requires the atm channel");
        }
        String boundTerminalId = callerContext.terminalId().orElse(null);
        if (boundTerminalId == null || !boundTerminalId.equals(terminalId)) {
            throw CallerIdentityException.invalid("Session is not bound to the presenting terminal");
        }

        MDC.put(USER_TOKEN_MDC_KEY, bearerToken);
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        MDC.remove(USER_TOKEN_MDC_KEY);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length());
    }
}
