package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coreservice.auth.infrastructure.rest.EnforcementFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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
     * 2. When enabled, a valid service credential is let through on a domain endpoint
     * 3. When enabled, a valid service credential is let through on a pre-auth endpoint
     * 4. When enabled, a missing service credential is rejected before reaching the chain
     * 5. When enabled, an unrecognized service credential is rejected before reaching the chain
     */

    private static final Map<String, String> CREDENTIALS = Map.of("web", "web-secret");

    @Test
    @DisplayName("when disabled, passes every request through unchanged, even with no credentials")
    void whenDisabledPassesEveryRequestThroughUnchanged() throws Exception {
        EnforcementFilter filter = new EnforcementFilter(false, Map.of());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("when enabled, a valid service credential is let through on a domain endpoint")
    void whenEnabledValidCredentialLetThroughOnDomainEndpoint() throws Exception {
        EnforcementFilter filter = new EnforcementFilter(true, CREDENTIALS);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        request.addHeader("X-Service-Credential", "web-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("when enabled, a valid service credential is let through on a pre-auth endpoint")
    void whenEnabledValidCredentialLetThroughOnPreAuthEndpoint() throws Exception {
        EnforcementFilter filter = new EnforcementFilter(true, CREDENTIALS);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/auth/web/refresh-tokens");
        request.addHeader("X-Service-Credential", "web-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("when enabled, a missing service credential is rejected before reaching the chain")
    void whenEnabledMissingCredentialRejected() throws Exception {
        EnforcementFilter filter = new EnforcementFilter(true, CREDENTIALS);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(401, response.getStatus());
    }

    @Test
    @DisplayName("when enabled, an unrecognized service credential is rejected before reaching the chain")
    void whenEnabledUnrecognizedCredentialRejected() throws Exception {
        EnforcementFilter filter = new EnforcementFilter(true, CREDENTIALS);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/accounts/any/balance");
        request.addHeader("X-Service-Credential", "not-a-real-credential");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(401, response.getStatus());
    }
}
