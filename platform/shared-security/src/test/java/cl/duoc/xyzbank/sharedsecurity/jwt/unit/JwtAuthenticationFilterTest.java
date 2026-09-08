package cl.duoc.xyzbank.sharedsecurity.jwt.unit;

import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtAuthenticationException;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtCallerAuthenticator;
import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.Hs256JwtFactory;
import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.JwtAuthenticationFilter;
import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.NimbusJwtTokenParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("The JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            new JwtCallerAuthenticator(
                    new NimbusJwtTokenParser(Hs256JwtFactory.DEFAULT_SECRET, Hs256JwtFactory.DEFAULT_ISSUER)),
            new ObjectMapper());

    @Test
    @DisplayName("does not fall back to identity headers when the bearer token is invalid")
    void doesNotFallBackToIdentityHeadersWhenTheBearerTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt");
        request.addHeader("X-Customer-Id", "customer-1");
        request.addHeader("X-Channel", "web");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("continues without authenticating when the authorization header is absent")
    void continuesWithoutAuthenticatingWhenTheAuthorizationHeaderIsAbsent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Customer-Id", "customer-1");
        request.addHeader("X-Channel", "web");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }
}
