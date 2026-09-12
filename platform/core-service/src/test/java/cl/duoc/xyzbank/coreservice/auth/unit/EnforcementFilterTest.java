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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The EnforcementFilter")
class EnforcementFilterTest {

    /*
     * Cases:
     * 1. When disabled, every request passes through unchanged, even with no credentials
     */

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
}
