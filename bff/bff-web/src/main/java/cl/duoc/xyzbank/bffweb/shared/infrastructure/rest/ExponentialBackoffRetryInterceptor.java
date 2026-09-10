package cl.duoc.xyzbank.bffweb.shared.infrastructure.rest;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Set;

public class ExponentialBackoffRetryInterceptor implements ClientHttpRequestInterceptor {

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(502, 503, 504);

    private final RetryPolicy policy;
    private final Sleeper sleeper;

    public ExponentialBackoffRetryInterceptor(RetryPolicy policy, Sleeper sleeper) {
        this.policy = policy;
        this.sleeper = sleeper;
    }

    public static ExponentialBackoffRetryInterceptor withThreadSleep(RetryPolicy policy) {
        return new ExponentialBackoffRetryInterceptor(policy, ExponentialBackoffRetryInterceptor::sleepUninterruptibly);
    }

    private static void sleepUninterruptibly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry backoff interrupted", interrupted);
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        int attempt = 1;
        while (true) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                if (!shouldRetry(request, response.getStatusCode(), attempt)) {
                    return response;
                }
                response.close();
            } catch (IOException exception) {
                if (!isRetryableGet(request, attempt)) {
                    throw exception;
                }
            }
            sleeper.sleep(backoffMs(attempt));
            attempt++;
        }
    }

    private boolean shouldRetry(HttpRequest request, HttpStatusCode status, int attempt) {
        return isRetryableGet(request, attempt) && RETRYABLE_STATUSES.contains(status.value());
    }

    private boolean isRetryableGet(HttpRequest request, int attempt) {
        return HttpMethod.GET.equals(request.getMethod()) && attempt < policy.maxAttempts();
    }

    private long backoffMs(int attempt) {
        double delay = policy.initialBackoffMs() * Math.pow(policy.multiplier(), attempt - 1);
        return Math.min(policy.maxBackoffMs(), Math.round(delay));
    }

    public record RetryPolicy(int maxAttempts, long initialBackoffMs, double multiplier, long maxBackoffMs) {
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis);
    }
}
