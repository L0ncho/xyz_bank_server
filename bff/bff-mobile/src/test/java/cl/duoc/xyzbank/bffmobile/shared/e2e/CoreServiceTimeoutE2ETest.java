package cl.duoc.xyzbank.bffmobile.shared.e2e;

import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.Hs256JwtFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The mobile BFF timeout mapping")
class CoreServiceTimeoutE2ETest {

    private static final WireMockServer CORE_SERVICE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        CORE_SERVICE.start();
    }

    @DynamicPropertySource
    static void timeouts(DynamicPropertyRegistry registry) {
        registry.add("core-service.base-url", CORE_SERVICE::baseUrl);
        registry.add("core-service.read-timeout-ms", () -> 200);
        registry.add("core-service.retry.max-attempts", () -> 1);
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        CORE_SERVICE.resetAll();
    }

    @AfterAll
    static void stopCoreServiceStub() {
        CORE_SERVICE.stop();
    }

    @Test
    @DisplayName("maps a delayed upstream GET to a 504 problem")
    void mapsADelayedUpstreamGetToA504Problem() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\":\"account-1\",\"balance\":500.00,\"currency\":\"USD\"}")));

        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(504)
                .contentType("application/problem+json")
                .body("title", equalTo("Gateway Timeout"))
                .body("detail", equalTo("The upstream service did not respond in time"));
    }
}
