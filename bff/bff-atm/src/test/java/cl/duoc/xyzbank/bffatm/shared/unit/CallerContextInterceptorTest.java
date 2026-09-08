package cl.duoc.xyzbank.bffatm.shared.unit;

import cl.duoc.xyzbank.bffatm.shared.infrastructure.rest.CallerContextInterceptor;
import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import cl.duoc.xyzbank.sharedsecurity.callercontext.HeaderCallerContextAdapter;
import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.CallerContextAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The ATM CallerContextInterceptor")
class CallerContextInterceptorTest {

    private final CallerContextInterceptor interceptor = new CallerContextInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("accepts an exact terminal header match")
    void acceptsAnExactTerminalHeaderMatch() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(CallerContextAuthentication.authenticated(
                        HeaderCallerContextAdapter.resolve("customer-1", "atm", "terminal-1"),
                        List.of("ROLE_ATM")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Terminal-Id", "terminal-1");

        boolean continued = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod());

        assertTrue(continued);
    }

    @Test
    @DisplayName("rejects a terminal header that does not equal the JWT claim")
    void rejectsATerminalHeaderThatDoesNotEqualTheJwtClaim() {
        SecurityContextHolder.getContext()
                .setAuthentication(CallerContextAuthentication.authenticated(
                        HeaderCallerContextAdapter.resolve("customer-1", "atm", "terminal-1"),
                        List.of("ROLE_ATM")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Terminal-Id", "terminal-2");

        CallerIdentityException exception = assertThrows(
                CallerIdentityException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod()));

        assertEquals(CallerIdentityException.Type.FORBIDDEN, exception.getType());
    }

    private static HandlerMethod handlerMethod() throws NoSuchMethodException {
        return new HandlerMethod(new Object(), Object.class.getMethod("toString"));
    }
}
