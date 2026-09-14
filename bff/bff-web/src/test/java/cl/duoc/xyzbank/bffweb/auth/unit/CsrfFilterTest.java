package cl.duoc.xyzbank.bffweb.auth.unit;

import cl.duoc.xyzbank.bffweb.auth.infrastructure.rest.CsrfFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The CsrfFilter")
class CsrfFilterTest {

    /*
     * Cases:
     * 1. A safe (GET) request needs no CSRF token
     * 2. A state-changing request with a matching cookie and header is let through
     * 3. A state-changing request with no CSRF cookie is rejected before reaching the chain
     * 4. A state-changing request with no CSRF header is rejected before reaching the chain
     * 5. A state-changing request whose cookie and header values don't match is rejected
     */

    private final CsrfFilter filter = new CsrfFilter();

    @Test
    @DisplayName("lets a safe GET request through with no CSRF token")
    void letsASafeGetRequestThroughWithNoCsrfToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("lets a state-changing request through when the cookie and header match")
    void letsAStateChangingRequestThroughWhenTheCookieAndHeaderMatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/session/refresh");
        request.setCookies(new Cookie("XSRF-TOKEN", "token-1"));
        request.addHeader("X-XSRF-TOKEN", "token-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("rejects a state-changing request with no CSRF cookie")
    void rejectsAStateChangingRequestWithNoCsrfCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/session/refresh");
        request.addHeader("X-XSRF-TOKEN", "token-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("rejects a state-changing request with no CSRF header")
    void rejectsAStateChangingRequestWithNoCsrfHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/session/refresh");
        request.setCookies(new Cookie("XSRF-TOKEN", "token-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("rejects a state-changing request whose cookie and header don't match")
    void rejectsAStateChangingRequestWhoseCookieAndHeaderDontMatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/session/refresh");
        request.setCookies(new Cookie("XSRF-TOKEN", "token-1"));
        request.addHeader("X-XSRF-TOKEN", "token-2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(403, response.getStatus());
    }
}
