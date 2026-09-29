package cl.duoc.xyzbank.bffweb.transactionhistory.e2e;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The transaction history circuit breaker")
class TransactionHistoryCircuitBreakerE2ETest {

    /*
     * 1. The breaker opens after repeated failures of the core transactions call
     */

    private static final String BREAKER_NAME = "coreServiceWeb";
    private static final WireMockServer CORE_SERVICE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        CORE_SERVICE.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("core-service.base-url", CORE_SERVICE::baseUrl);
        registry.add("resilience4j.circuitbreaker.instances.coreServiceWeb.minimumNumberOfCalls", () -> "2");
        registry.add("resilience4j.circuitbreaker.instances.coreServiceWeb.slidingWindowSize", () -> "4");
        registry.add("resilience4j.circuitbreaker.instances.coreServiceWeb.failureRateThreshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.coreServiceWeb.waitDurationInOpenState", () -> "2s");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private JwtCallerContextAdapter tokenAdapter;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.useRelaxedHTTPSValidation();
        CORE_SERVICE.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
    }

    @AfterAll
    static void stopCoreServiceStub() {
        CORE_SERVICE.stop();
    }

    @Test
    @DisplayName("opens the circuit breaker after repeated core failures")
    void opensTheCircuitBreakerAfterRepeatedCoreFailures() {
        CORE_SERVICE.stubFor(get(urlPathEqualTo("/internal/accounts/account-1/transactions"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(BREAKER_NAME);
        String session = tokenAdapter.issue("customer-1", Channel.WEB, null);

        for (int attempt = 0; attempt < 3; attempt++) {
            given()
                    .cookie("session", session)
                    .when()
                    .get("/accounts/{accountId}/transactions", "account-1")
                    .then()
                    .statusCode(500);
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
}
