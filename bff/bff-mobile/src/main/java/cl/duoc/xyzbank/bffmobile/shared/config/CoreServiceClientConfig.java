package cl.duoc.xyzbank.bffmobile.shared.config;

import cl.duoc.xyzbank.bffmobile.shared.infrastructure.rest.CorrelationIdClientInterceptor;
import cl.duoc.xyzbank.bffmobile.shared.infrastructure.rest.ExponentialBackoffRetryInterceptor;
import cl.duoc.xyzbank.bffmobile.shared.infrastructure.rest.ExponentialBackoffRetryInterceptor.RetryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CoreServiceClientConfig {

    @Bean
    public ExponentialBackoffRetryInterceptor exponentialBackoffRetryInterceptor(
            @Value("${core-service.retry.max-attempts:3}") int maxAttempts,
            @Value("${core-service.retry.initial-backoff-ms:200}") long initialBackoffMs,
            @Value("${core-service.retry.multiplier:2.0}") double multiplier,
            @Value("${core-service.retry.max-backoff-ms:2000}") long maxBackoffMs) {
        return ExponentialBackoffRetryInterceptor.withThreadSleep(
                new RetryPolicy(maxAttempts, initialBackoffMs, multiplier, maxBackoffMs));
    }

    @Bean
    public RestClient coreServiceClient(
            @Value("${core-service.base-url}") String baseUrl,
            @Value("${core-service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${core-service.read-timeout-ms}") int readTimeoutMs,
            CorrelationIdClientInterceptor correlationIdClientInterceptor,
            ExponentialBackoffRetryInterceptor exponentialBackoffRetryInterceptor) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(correlationIdClientInterceptor)
                .requestInterceptor(exponentialBackoffRetryInterceptor)
                .build();
    }
}
