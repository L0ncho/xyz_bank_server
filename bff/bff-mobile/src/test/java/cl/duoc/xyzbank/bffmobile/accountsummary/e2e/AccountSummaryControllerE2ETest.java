package cl.duoc.xyzbank.bffmobile.accountsummary.e2e;

import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.Hs256JwtFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The Account Summary controller")
class AccountSummaryControllerE2ETest {

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

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        CORE_SERVICE.resetAll();
        stubSummary();
    }

    @AfterAll
    static void stopCoreServiceStub() {
        CORE_SERVICE.stop();
    }

    @Test
    @DisplayName("returns a flat account summary without transaction history")
    void returnsAFlatAccountSummaryWithoutTransactionHistory() {
        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(200)
                .body("accountId", equalTo("account-1"))
                .body("balance", equalTo(500.00f))
                .body("currency", equalTo("USD"))
                .body("transactions", nullValue())
                .body("profile", nullValue())
                .body("nextCursor", nullValue());
    }

    @Test
    @DisplayName("maps an upstream 500 to a 502 problem without internal traces")
    void mapsAnUpstream500ToA502ProblemWithoutInternalTraces() {
        CORE_SERVICE.resetAll();
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/balance"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"detail\":\"boom\",\"trace\":\"java.lang.IllegalStateException\"}")));

        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(502)
                .contentType("application/problem+json")
                .body("title", equalTo("Bad Gateway"))
                .body("detail", equalTo("The upstream service failed"))
                .body("detail", not(containsString("IllegalStateException")));
    }

    @Test
    @DisplayName("responds with not-found for an unknown account")
    void respondsWithNotFoundForAnUnknownAccount() {
        CORE_SERVICE.resetAll();
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/unknown/balance"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"detail\":\"Account unknown not found\"}")));

        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .when()
                .get("/accounts/{accountId}/summary", "unknown")
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("rejects a caller whose channel is not mobile")
    void rejectsACallerWhoseChannelIsNotMobile() {
        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "web"))
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(403)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("ignores filter and pagination query parameters")
    void ignoresFilterAndPaginationQueryParameters() {
        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .queryParam("from", "2020-01-01")
                .queryParam("to", "2026-12-31")
                .queryParam("type", "CREDIT")
                .queryParam("cursor", "ignored")
                .queryParam("pageSize", "100")
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(200)
                .body("accountId", equalTo("account-1"))
                .body("transactions", nullValue())
                .body("nextCursor", nullValue());
    }

    @Test
    @DisplayName("propagates the correlation id to core-service")
    void propagatesTheCorrelationIdToCoreService() {
        given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .header("X-Correlation-Id", "corr-mobile-1")
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", equalTo("corr-mobile-1"));

        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/accounts/account-1/balance"))
                .withHeader("X-Correlation-Id", WireMock.equalTo("corr-mobile-1")));
        CORE_SERVICE.verify(0, getRequestedFor(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5")));
    }

    @Test
    @DisplayName("propagates a generated correlation id when the inbound header is absent")
    void propagatesAGeneratedCorrelationIdWhenTheInboundHeaderIsAbsent() {
        String correlationId = given()
                .header("Authorization", "Bearer " + Hs256JwtFactory.devToken("customer-1", "mobile"))
                .when()
                .get("/accounts/{accountId}/summary", "account-1")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", not(emptyOrNullString()))
                .extract()
                .header("X-Correlation-Id");

        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/accounts/account-1/balance"))
                .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId)));
        CORE_SERVICE.verify(0, getRequestedFor(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5")));
    }

    private static void stubSummary() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\":\"account-1\",\"balance\":500.00,\"currency\":\"USD\"}")));
    }
}
