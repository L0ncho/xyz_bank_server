package cl.duoc.xyzbank.bffmobile.shared.unit;

import cl.duoc.xyzbank.bffmobile.shared.infrastructure.adapters.CoreServiceCallException;
import cl.duoc.xyzbank.bffmobile.shared.infrastructure.adapters.CoreServiceCalls;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The core-service call mapper")
class CoreServiceCallsTest {

    @Test
    @DisplayName("maps a 404 to a not-found problem without the upstream body")
    void mapsA404ToANotFoundProblemWithoutTheUpstreamBody() {
        CoreServiceCallException exception = assertThrows(
                CoreServiceCallException.class,
                () -> CoreServiceCalls.fetch(() -> {
                    throw HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND,
                            "Not Found",
                            HttpHeaders.EMPTY,
                            "{\"detail\":\"Account secret-internal not found\",\"trace\":\"boom\"}".getBytes(),
                            null);
                }));

        assertEquals(404, exception.getStatus());
        assertEquals("The requested resource was not found", exception.getMessage());
    }

    @Test
    @DisplayName("maps an upstream 500 to 502")
    void mapsAnUpstream500To502() {
        CoreServiceCallException exception = assertThrows(
                CoreServiceCallException.class,
                () -> CoreServiceCalls.fetch(() -> {
                    throw HttpServerErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error",
                            HttpHeaders.EMPTY,
                            "{\"trace\":\"java.lang.RuntimeException\"}".getBytes(),
                            null);
                }));

        assertEquals(502, exception.getStatus());
        assertEquals("The upstream service failed", exception.getMessage());
    }

    @Test
    @DisplayName("maps a timeout to 504")
    void mapsATimeoutTo504() {
        CoreServiceCallException exception = assertThrows(
                CoreServiceCallException.class,
                () -> CoreServiceCalls.fetch(() -> {
                    throw new ResourceAccessException("Read timed out");
                }));

        assertEquals(504, exception.getStatus());
        assertEquals("The upstream service did not respond in time", exception.getMessage());
    }
}
