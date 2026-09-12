package cl.duoc.xyzbank.bffweb.dashboard.e2e;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The Dashboard controller")
class DashboardControllerE2ETest {

    /*
     * Cases:
     * 1. Successful dashboard aggregate
     * 2. Unknown customer is not found
     * 3. Non-web channel is rejected
     * 4. Outbound core-service calls carry the inbound correlation id
     * 5. A generated correlation id is forwarded when the inbound header is absent
     * 6. Every outbound core-service call carries the service credential
     */

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
        RestAssured.useRelaxedHTTPSValidation();
        CORE_SERVICE.resetAll();
    }

    private String webSessionFor(String customerId) {
        return tokenAdapter.issue(customerId, Channel.WEB, null);
    }

    @AfterAll
    static void stopCoreServiceStub() {
        CORE_SERVICE.stop();
    }

    @Test
    @DisplayName("aggregates profile, accounts, and latest transactions")
    void aggregatesProfileAccountsAndLatestTransactions() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .willReturn(json("{\"id\":\"customer-1\",\"fullName\":\"Ana Perez\",\"email\":\"ana@example.com\"}")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1/accounts"))
                .willReturn(json(
                        "[{\"id\":\"account-1\",\"accountNumber\":\"1000000001\",\"balance\":500.00,\"currency\":\"USD\"}]")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .willReturn(json(
                        "{\"items\":[{\"id\":\"tx-1\",\"type\":\"DEBIT\",\"amount\":50.00,\"currency\":\"USD\",\"occurredOn\":\"2026-01-01\",\"description\":null}],\"nextCursor\":null}")));

        given()
                .cookie("session", webSessionFor("customer-1"))
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(200)
                .body("profile.id", equalTo("customer-1"))
                .body("profile.fullName", equalTo("Ana Perez"))
                .body("accounts", hasSize(1))
                .body("accounts[0].id", equalTo("account-1"))
                .body("accounts[0].transactions", hasSize(1))
                .body("accounts[0].transactions[0].id", equalTo("tx-1"));
    }

    @Test
    @DisplayName("responds with not-found for an unknown customer")
    void respondsWithNotFoundForAnUnknownCustomer() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/unknown"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"detail\":\"Customer unknown not found\"}")));

        given()
                .cookie("session", webSessionFor("unknown"))
                .when()
                .get("/customers/{customerId}/dashboard", "unknown")
                .then()
                .statusCode(404)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("rejects a caller whose channel is not web")
    void rejectsACallerWhoseChannelIsNotWeb() {
        given()
                .cookie("session", tokenAdapter.issue("customer-1", Channel.MOBILE, null))
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(403)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("rejects a request with no session cookie")
    void rejectsARequestWithNoSessionCookie() {
        given()
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(422)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("rejects a request with an expired session cookie")
    void rejectsARequestWithAnExpiredSessionCookie() {
        JwtCallerContextAdapter expiredTokenAdapter = new JwtCallerContextAdapter(
                "dev-channel-auth-jwt-signing-secret-please-rotate-in-prod",
                java.time.Clock.fixed(java.time.Instant.parse("2020-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));

        given()
                .cookie("session", expiredTokenAdapter.issue("customer-1", Channel.WEB, null))
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(422)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("propagates the correlation id to every core-service call")
    void propagatesTheCorrelationIdToEveryCoreServiceCall() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .willReturn(json("{\"id\":\"customer-1\",\"fullName\":\"Ana Perez\",\"email\":\"ana@example.com\"}")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1/accounts"))
                .willReturn(json(
                        "[{\"id\":\"account-1\",\"accountNumber\":\"1000000001\",\"balance\":500.00,\"currency\":\"USD\"}]")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .willReturn(json(
                        "{\"items\":[],\"nextCursor\":null}")));

        given()
                .cookie("session", webSessionFor("customer-1"))
                .header("X-Correlation-Id", "corr-web-1")
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", equalTo("corr-web-1"));

        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/customers/customer-1"))
                .withHeader("X-Correlation-Id", WireMock.equalTo("corr-web-1")));
        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/customers/customer-1/accounts"))
                .withHeader("X-Correlation-Id", WireMock.equalTo("corr-web-1")));
        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .withHeader("X-Correlation-Id", WireMock.equalTo("corr-web-1")));
    }

    @Test
    @DisplayName("propagates a generated correlation id when the inbound header is absent")
    void propagatesAGeneratedCorrelationIdWhenTheInboundHeaderIsAbsent() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .willReturn(json("{\"id\":\"customer-1\",\"fullName\":\"Ana Perez\",\"email\":\"ana@example.com\"}")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1/accounts"))
                .willReturn(json(
                        "[{\"id\":\"account-1\",\"accountNumber\":\"1000000001\",\"balance\":500.00,\"currency\":\"USD\"}]")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .willReturn(json("{\"items\":[],\"nextCursor\":null}")));

        String correlationId = given()
                .cookie("session", webSessionFor("customer-1"))
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(200)
                .header("X-Correlation-Id", not(emptyOrNullString()))
                .extract()
                .header("X-Correlation-Id");

        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/customers/customer-1"))
                .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId)));
        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/customers/customer-1/accounts"))
                .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId)));
        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .withHeader("X-Correlation-Id", WireMock.equalTo(correlationId)));
    }

    @Test
    @DisplayName("carries the service credential on every outbound core-service call")
    void carriesTheServiceCredentialOnEveryOutboundCoreServiceCall() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1"))
                .willReturn(json("{\"id\":\"customer-1\",\"fullName\":\"Ana Perez\",\"email\":\"ana@example.com\"}")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/customers/customer-1/accounts"))
                .willReturn(json(
                        "[{\"id\":\"account-1\",\"accountNumber\":\"1000000001\",\"balance\":500.00,\"currency\":\"USD\"}]")));
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .willReturn(json("{\"items\":[],\"nextCursor\":null}")));

        given()
                .cookie("session", webSessionFor("customer-1"))
                .when()
                .get("/customers/{customerId}/dashboard", "customer-1")
                .then()
                .statusCode(200);

        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/customers/customer-1"))
                .withHeader("X-Service-Credential", WireMock.equalTo("dev-service-credential-web")));
        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/customers/customer-1/accounts"))
                .withHeader("X-Service-Credential", WireMock.equalTo("dev-service-credential-web")));
        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/accounts/account-1/transactions?pageSize=5"))
                .withHeader("X-Service-Credential", WireMock.equalTo("dev-service-credential-web")));
    }

    private static ResponseDefinitionBuilder json(String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
