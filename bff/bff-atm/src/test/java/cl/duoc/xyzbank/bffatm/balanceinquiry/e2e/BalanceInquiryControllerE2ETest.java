package cl.duoc.xyzbank.bffatm.balanceinquiry.e2e;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.config.SSLConfig;
import io.restassured.specification.RequestSpecification;
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
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The Balance Inquiry controller")
class BalanceInquiryControllerE2ETest {

    private static final String TERMINAL_ID = "atm-terminal-001";

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

    private RequestSpecification asAtm() {
        return given().header("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.ATM, TERMINAL_ID));
    }

    @Test
    @DisplayName("returns the account balance")
    void returnsTheAccountBalance() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\":\"account-1\",\"balance\":250.00,\"currency\":\"USD\"}")));

        asAtm()
                .when()
                .get("/accounts/{accountId}/balance", "account-1")
                .then()
                .statusCode(200)
                .body("balance", equalTo(250.00f))
                .body("currency", equalTo("USD"));
    }

    @Test
    @DisplayName("rejects a caller whose channel is not atm")
    void rejectsACallerWhoseChannelIsNotAtm() {
        given()
                .header("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.WEB, null))
                .when()
                .get("/accounts/{accountId}/balance", "account-1")
                .then()
                .statusCode(403)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("carries the service credential on every outbound core-service call")
    void carriesTheServiceCredentialOnEveryOutboundCoreServiceCall() {
        CORE_SERVICE.stubFor(get(urlEqualTo("/internal/accounts/account-1/balance"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"accountId\":\"account-1\",\"balance\":250.00,\"currency\":\"USD\"}")));

        asAtm()
                .when()
                .get("/accounts/{accountId}/balance", "account-1")
                .then()
                .statusCode(200);

        CORE_SERVICE.verify(getRequestedFor(urlEqualTo("/internal/accounts/account-1/balance"))
                .withHeader("X-Service-Credential", WireMock.equalTo("dev-service-credential-atm")));
    }

    @Test
    @DisplayName("rejects a missing bearer token")
    void rejectsAMissingBearerToken() {
        given()
                .when()
                .get("/accounts/{accountId}/balance", "account-1")
                .then()
                .statusCode(422)
                .contentType("application/problem+json");
    }

    @Test
    @DisplayName("rejects a session bound to a different terminal than the one presenting it")
    void rejectsASessionBoundToADifferentTerminal() {
        given()
                .header("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.ATM, "some-other-terminal"))
                .when()
                .get("/accounts/{accountId}/balance", "account-1")
                .then()
                .statusCode(422)
                .contentType("application/problem+json");
    }
}
