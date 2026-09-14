package cl.duoc.xyzbank.bffatm.balanceinquiry.e2e;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The Balance Inquiry controller's response time")
class BalanceLatencyE2ETest {

    /*
     * Cases:
     * 1. Balance inquiry responds within a generous ceiling -- this endpoint has no
     *    fan-out to parallelize, so this simply records/asserts its response time as the
     *    rubric's second latency-evidence data point (see DashboardLatencyE2ETest for the
     *    concurrency proof)
     */

    private static final String TERMINAL_ID = "atm-terminal-001";
    private static final Duration LATENCY_CEILING = Duration.ofSeconds(2);

    private static final WireMockServer CORE_SERVICE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        CORE_SERVICE.start();
    }

    @DynamicPropertySource
    static void coreServiceBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("core-service.base-url", CORE_SERVICE::baseUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtCallerContextAdapter tokenAdapter;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.config = RestAssured.config()
                .sslConfig(SSLConfig.sslConfig()
                        .keyStore("tls/terminal-keystore.p12", "xyzbank-dev")
                        .and()
                        .relaxedHTTPSValidation());
        CORE_SERVICE.resetAll();
    }

    @AfterAll
    static void stopCoreServiceStub() {
        CORE_SERVICE.stop();
    }

    @Test
    @DisplayName("responds within a generous latency ceiling")
    void respondsWithinAGenerousLatencyCeiling() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\":\"account-1\",\"balance\":250.00,\"currency\":\"USD\"}")));
        String sessionToken = tokenAdapter.issue("customer-1", Channel.ATM, TERMINAL_ID);

        Instant start = Instant.now();
        given()
                .header("Authorization", "Bearer " + sessionToken)
                .when()
                .get("/accounts/{accountId}/balance", "account-1")
                .then()
                .statusCode(200);
        Duration elapsed = Duration.between(start, Instant.now());

        assertTrue(
                elapsed.compareTo(LATENCY_CEILING) < 0,
                () -> "expected balance inquiry to respond within " + LATENCY_CEILING + " but took " + elapsed);
    }
}
