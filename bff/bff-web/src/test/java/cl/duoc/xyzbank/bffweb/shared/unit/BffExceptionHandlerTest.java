package cl.duoc.xyzbank.bffweb.shared.unit;

import cl.duoc.xyzbank.bffweb.shared.infrastructure.adapters.CoreServiceCallException;
import cl.duoc.xyzbank.bffweb.shared.infrastructure.rest.BffExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("The BFF exception handler")
class BffExceptionHandlerTest {

    private final BffExceptionHandler handler = new BffExceptionHandler();

    @Test
    @DisplayName("builds a RFC 7807 problem for an upstream failure")
    void buildsARfc7807ProblemForAnUpstreamFailure() {
        ProblemDetail problem =
                handler.handleCoreServiceCall(new CoreServiceCallException(502, "The upstream service failed"));

        assertEquals(502, problem.getStatus());
        assertEquals("Bad Gateway", problem.getTitle());
        assertEquals("The upstream service failed", problem.getDetail());
        assertFalse(String.valueOf(problem.getDetail()).contains("Exception"));
    }

    @Test
    @DisplayName("builds a RFC 7807 problem for a gateway timeout")
    void buildsARfc7807ProblemForAGatewayTimeout() {
        ProblemDetail problem = handler.handleCoreServiceCall(
                new CoreServiceCallException(504, "The upstream service did not respond in time"));

        assertEquals(504, problem.getStatus());
        assertEquals("Gateway Timeout", problem.getTitle());
        assertEquals("The upstream service did not respond in time", problem.getDetail());
    }
}
