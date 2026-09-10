package cl.duoc.xyzbank.bffatm.shared.unit;

import cl.duoc.xyzbank.bffatm.shared.infrastructure.adapters.CoreServiceCallException;
import cl.duoc.xyzbank.bffatm.shared.infrastructure.adapters.CoreServiceCalls;
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
    @DisplayName("relays 409 and 422 with the same status")
    void relays409And422WithTheSameStatus() {
        CoreServiceCallException conflict = assertThrows(
                CoreServiceCallException.class,
                () -> CoreServiceCalls.fetch(() -> {
                    throw HttpClientErrorException.create(
                            HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, new byte[0], null);
                }));
        CoreServiceCallException unprocessable = assertThrows(
                CoreServiceCallException.class,
                () -> CoreServiceCalls.fetch(() -> {
                    throw HttpClientErrorException.create(
                            HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable", HttpHeaders.EMPTY, new byte[0], null);
                }));

        assertEquals(409, conflict.getStatus());
        assertEquals(422, unprocessable.getStatus());
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
