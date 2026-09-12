package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coreservice.auth.infrastructure.rest.EnforcementFilter;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The EnforcementFilter")
class EnforcementFilterTest {

    /*
     * Cases:
     * 1. When disabled, every request passes through unchanged, even with no credentials
     * 2. When enabled, a valid service credential is let through on a domain endpoint with a sufficiently scoped user token
     * 3. When enabled, a valid service credential is let through on a pre-auth endpoint (no user token needed)
     * 4. When enabled, a missing service credential is rejected before reaching the chain
     * 5. When enabled, an unrecognized service credential is rejected before reaching the chain
     * 6. When enabled, a domain endpoint with a valid service credential but no user token is rejected
     * 7. When enabled, a domain endpoint with an expired user token is rejected
     * 8. When enabled, a domain endpoint with a user token lacking the required scope is rejected
     */

    private static final String SECRET = "unit-test-signing-secret-unit-test-signing-secret";
    private static final Map<String, String> CREDENTIALS = Map.of("web", "web-secret");

    private final JwtCallerContextAdapter tokenAdapter = new JwtCallerContextAdapter(SECRET);

    private EnforcementFilter filter(boolean enabled) {
        return new EnforcementFilter(enabled, CREDENTIALS, tokenAdapter);
    }

    @Test
    @DisplayName("when disabled, passes every request through unchanged, even with no credentials")
    void whenDisabledPassesEveryRequestThroughUnchanged() throws Exception {
        EnforcementFilter filter = new EnforcementFilter(false, Map.of(), tokenAdapter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("when enabled, a valid credential and a sufficiently scoped token are let through on a domain endpoint")
    void whenEnabledValidCredentialAndScopedTokenLetThroughOnDomainEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.WEB, null));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("when enabled, a valid service credential is let through on a pre-auth endpoint")
    void whenEnabledValidCredentialLetThroughOnPreAuthEndpoint() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/web/refresh-tokens");
        request.addHeader("X-Service-Credential", "web-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("when enabled, a missing service credential is rejected before reaching the chain")
    void whenEnabledMissingCredentialRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("when enabled, an unrecognized service credential is rejected before reaching the chain")
    void whenEnabledUnrecognizedCredentialRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        request.addHeader("X-Service-Credential", "not-a-real-credential");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("when enabled, a domain endpoint with no user token is rejected")
    void whenEnabledDomainEndpointWithNoUserTokenRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        request.addHeader("X-Service-Credential", "web-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertFalse(chainCalled.get());
    }

    @Test
    @DisplayName("when enabled, a domain endpoint with an expired user token is rejected")
    void whenEnabledDomainEndpointWithExpiredUserTokenRejected() throws Exception {
        JwtCallerContextAdapter expiredTokenAdapter =
                new JwtCallerContextAdapter(SECRET, Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));
        String expiredToken = expiredTokenAdapter.issue("customer-1", Channel.WEB, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + expiredToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertFalse(chainCalled.get());
    }

    @Test
    @DisplayName("when enabled, a domain endpoint with a user token lacking the required scope is rejected")
    void whenEnabledDomainEndpointWithInsufficientScopeRejected() throws Exception {
        // /internal/customers/{id} requires web:customers:read; a mobile token never has it
        String mobileToken = tokenAdapter.issue("customer-1", Channel.MOBILE, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/customers/any");
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + mobileToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter(true).doFilter(request, response, chain);

        assertFalse(chainCalled.get());
    }
}
