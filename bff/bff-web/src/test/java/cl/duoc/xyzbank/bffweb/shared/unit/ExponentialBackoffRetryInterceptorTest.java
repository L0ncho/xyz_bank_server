package cl.duoc.xyzbank.bffweb.shared.unit;

import cl.duoc.xyzbank.bffweb.shared.infrastructure.rest.ExponentialBackoffRetryInterceptor;
import cl.duoc.xyzbank.bffweb.shared.infrastructure.rest.ExponentialBackoffRetryInterceptor.RetryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The exponential backoff retry interceptor")
class ExponentialBackoffRetryInterceptorTest {

    private static final RetryPolicy POLICY = new RetryPolicy(3, 200, 2.0, 2000);

    @Test
    @DisplayName("retries a GET that returns 503 then succeeds")
    void retriesAGetThatReturns503ThenSucceeds() throws IOException {
        ScriptedExecution execution = new ScriptedExecution(
                response(HttpStatus.SERVICE_UNAVAILABLE), response(HttpStatus.OK));
        List<Long> sleeps = new ArrayList<>();

        ClientHttpResponse response = interceptor(sleeps).intercept(getRequest(), new byte[0], execution);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertEquals(2, execution.calls);
        assertEquals(List.of(200L), sleeps);
    }

    @Test
    @DisplayName("stops after max attempts and returns the last GET failure")
    void stopsAfterMaxAttemptsAndReturnsTheLastGetFailure() throws IOException {
        ScriptedExecution execution = new ScriptedExecution(
                response(HttpStatus.BAD_GATEWAY),
                response(HttpStatus.SERVICE_UNAVAILABLE),
                response(HttpStatus.GATEWAY_TIMEOUT));
        List<Long> sleeps = new ArrayList<>();

        ClientHttpResponse response = interceptor(sleeps).intercept(getRequest(), new byte[0], execution);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), response.getStatusCode().value());
        assertEquals(3, execution.calls);
        assertEquals(List.of(200L, 400L), sleeps);
    }

    @Test
    @DisplayName("retries a GET IOException then succeeds")
    void retriesAGetIoExceptionThenSucceeds() throws IOException {
        ScriptedExecution execution = new ScriptedExecution(new IOException("timeout"), response(HttpStatus.OK));
        List<Long> sleeps = new ArrayList<>();

        ClientHttpResponse response = interceptor(sleeps).intercept(getRequest(), new byte[0], execution);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertEquals(2, execution.calls);
        assertEquals(List.of(200L), sleeps);
    }

    @Test
    @DisplayName("does not retry a GET 404")
    void doesNotRetryAGet404() throws IOException {
        ScriptedExecution execution = new ScriptedExecution(response(HttpStatus.NOT_FOUND));
        List<Long> sleeps = new ArrayList<>();

        ClientHttpResponse response = interceptor(sleeps).intercept(getRequest(), new byte[0], execution);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals(1, execution.calls);
        assertEquals(List.of(), sleeps);
    }

    @Test
    @DisplayName("does not retry a POST even when the response is 503")
    void doesNotRetryAPostEvenWhenTheResponseIs503() throws IOException {
        ScriptedExecution execution = new ScriptedExecution(response(HttpStatus.SERVICE_UNAVAILABLE));
        List<Long> sleeps = new ArrayList<>();

        ClientHttpResponse response = interceptor(sleeps).intercept(postRequest(), new byte[0], execution);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertEquals(1, execution.calls);
        assertEquals(List.of(), sleeps);
    }

    @Test
    @DisplayName("rethrows the last GET IOException after max attempts")
    void rethrowsTheLastGetIoExceptionAfterMaxAttempts() {
        ScriptedExecution execution = new ScriptedExecution(
                new IOException("one"), new IOException("two"), new IOException("three"));

        IOException exception = assertThrows(
                IOException.class, () -> interceptor(new ArrayList<>()).intercept(getRequest(), new byte[0], execution));

        assertEquals("three", exception.getMessage());
        assertEquals(3, execution.calls);
    }

    private static ExponentialBackoffRetryInterceptor interceptor(List<Long> sleeps) {
        return new ExponentialBackoffRetryInterceptor(POLICY, sleeps::add);
    }

    private static MockClientHttpRequest getRequest() {
        return new MockClientHttpRequest(HttpMethod.GET, URI.create("http://core/internal/accounts/1/balance"));
    }

    private static MockClientHttpRequest postRequest() {
        return new MockClientHttpRequest(HttpMethod.POST, URI.create("http://core/internal/accounts/1/withdrawals"));
    }

    private static MockClientHttpResponse response(HttpStatus status) {
        return new MockClientHttpResponse(new byte[0], status);
    }

    private static final class ScriptedExecution implements ClientHttpRequestExecution {
        private final Object[] outcomes;
        private int index;
        private int calls;

        private ScriptedExecution(Object... outcomes) {
            this.outcomes = outcomes;
        }

        @Override
        public ClientHttpResponse execute(org.springframework.http.HttpRequest request, byte[] body)
                throws IOException {
            calls++;
            Object next = outcomes[index++];
            if (next instanceof IOException exception) {
                throw exception;
            }
            return (ClientHttpResponse) next;
        }
    }
}
