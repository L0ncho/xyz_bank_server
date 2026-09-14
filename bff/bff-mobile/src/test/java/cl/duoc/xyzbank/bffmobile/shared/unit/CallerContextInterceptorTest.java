package cl.duoc.xyzbank.bffmobile.shared.unit;

import cl.duoc.xyzbank.bffmobile.shared.infrastructure.rest.CallerContextInterceptor;
import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The CallerContextInterceptor")
class CallerContextInterceptorTest {

    /*
     * Cases:
     * 1. A valid mobile token with a matching X-Device-Id header is let through
     * 2. Missing bearer token is rejected
     * 3. A non-mobile channel token is rejected
     * 4. A valid token presented with a device id that does not match the one it was bound
     *    to at login is rejected
     * 5. A valid token presented with no X-Device-Id header at all is rejected
     */

    private static final String SECRET = "unit-test-signing-secret-unit-test-signing-secret";

    private final JwtCallerContextAdapter tokenAdapter = new JwtCallerContextAdapter(SECRET);
    private final CallerContextInterceptor interceptor = new CallerContextInterceptor(tokenAdapter);

    private HandlerMethod aHandlerMethod() throws NoSuchMethodException {
        return new HandlerMethod(new DummyController(), "handle");
    }

    @Test
    @DisplayName("lets a valid mobile token with a matching device id through")
    void letsAValidMobileTokenWithAMatchingDeviceIdThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.MOBILE, "device-1"));
        request.addHeader("X-Device-Id", "device-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, aHandlerMethod()));
    }

    @Test
    @DisplayName("rejects a request with no bearer token")
    void rejectsARequestWithNoBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Device-Id", "device-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CallerIdentityException exception = assertThrows(
                CallerIdentityException.class, () -> interceptor.preHandle(request, response, aHandlerMethod()));
        assertEquals(CallerIdentityException.Type.INVALID, exception.getType());
    }

    @Test
    @DisplayName("rejects a non-mobile channel token")
    void rejectsANonMobileChannelToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.WEB, null));
        request.addHeader("X-Device-Id", "device-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CallerIdentityException exception = assertThrows(
                CallerIdentityException.class, () -> interceptor.preHandle(request, response, aHandlerMethod()));
        assertEquals(CallerIdentityException.Type.FORBIDDEN, exception.getType());
    }

    @Test
    @DisplayName("rejects a token presented with a device id that does not match the one it was bound to")
    void rejectsATokenPresentedWithAMismatchedDeviceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.MOBILE, "device-1"));
        request.addHeader("X-Device-Id", "device-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        CallerIdentityException exception = assertThrows(
                CallerIdentityException.class, () -> interceptor.preHandle(request, response, aHandlerMethod()));
        assertEquals(CallerIdentityException.Type.INVALID, exception.getType());
    }

    @Test
    @DisplayName("rejects a valid token presented with no X-Device-Id header at all")
    void rejectsAValidTokenPresentedWithNoDeviceIdHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.MOBILE, "device-1"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(
                CallerIdentityException.class, () -> interceptor.preHandle(request, response, aHandlerMethod()));
    }

    static class DummyController {
        public void handle() {
        }
    }
}
