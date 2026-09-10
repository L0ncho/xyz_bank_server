package cl.duoc.xyzbank.bffmobile.shared.unit;

import cl.duoc.xyzbank.bffmobile.shared.infrastructure.adapters.CoreServiceCallException;
import cl.duoc.xyzbank.bffmobile.shared.infrastructure.rest.BffExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }
}
