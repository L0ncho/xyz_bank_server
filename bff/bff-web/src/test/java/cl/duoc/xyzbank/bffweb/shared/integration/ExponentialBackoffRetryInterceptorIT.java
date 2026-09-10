package cl.duoc.xyzbank.bffweb.shared.integration;

import cl.duoc.xyzbank.bffweb.dashboard.application.dto.CustomerProfile;
import cl.duoc.xyzbank.bffweb.dashboard.infrastructure.adapters.HttpCustomerProfileAdapter;
import cl.duoc.xyzbank.bffweb.shared.infrastructure.rest.ExponentialBackoffRetryInterceptor;
import cl.duoc.xyzbank.bffweb.shared.infrastructure.rest.ExponentialBackoffRetryInterceptor.RetryPolicy;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("The exponential backoff retry interceptor against core-service")
class ExponentialBackoffRetryInterceptorIT {

    private WireMockServer wireMockServer;
    private HttpCustomerProfileAdapter adapter;

    @BeforeEach
    void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        RestClient client = RestClient.builder()
                .baseUrl(wireMockServer.baseUrl())
                .requestInterceptor(new ExponentialBackoffRetryInterceptor(
                        new RetryPolicy(3, 200, 2.0, 2000), millis -> {
                        }))
                .build();
        adapter = new HttpCustomerProfileAdapter(client);
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("retries two GET 503 responses and then returns the successful body")
    void retriesTwoGet503ResponsesAndThenReturnsTheSuccessfulBody() {
        wireMockServer.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .inScenario("retry-get")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("second")
                .willReturn(aResponse().withStatus(503)));
        wireMockServer.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .inScenario("retry-get")
                .whenScenarioStateIs("second")
                .willSetStateTo("ok")
                .willReturn(aResponse().withStatus(503)));
        wireMockServer.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .inScenario("retry-get")
                .whenScenarioStateIs("ok")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"customer-1\",\"fullName\":\"Ana Perez\",\"email\":\"ana@example.com\"}")));

        CustomerProfile profile = adapter.fetchProfile("customer-1");

        assertEquals(new CustomerProfile("customer-1", "Ana Perez", "ana@example.com"), profile);
        wireMockServer.verify(3, getRequestedFor(urlEqualTo("/internal/customers/customer-1")));
    }
}
