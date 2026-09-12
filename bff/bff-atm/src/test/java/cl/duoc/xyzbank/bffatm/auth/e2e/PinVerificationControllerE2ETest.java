package cl.duoc.xyzbank.bffatm.auth.e2e;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerContext;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
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

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The PIN verification endpoint")
class PinVerificationControllerE2ETest {

    /*
     * Cases:
     * 1. A correct PIN issues a session bound to the terminal's certificate
     * 2. An incorrect PIN is rejected with no session issued
     * 3. An unknown card is rejected identically to an incorrect PIN (no oracle)
     * 4. The PIN is placed only in the request body, never the request URI, and the call to
     *    core-service is made over TLS
     */

    private static final WireMockServer CORE_SERVICE = new WireMockServer(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort()
            .keystorePath("tls/keystore.p12")
            .keystorePassword("xyzbank-dev")
            .keyManagerPassword("xyzbank-dev"));

    static {
        CORE_SERVICE.start();
    }

    @DynamicPropertySource
    static void coreServicePinVerificationBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("core-service.pin-verification-base-url", () -> "https://localhost:" + CORE_SERVICE.httpsPort());
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
    @DisplayName("issues a session bound to the terminal for a correct pin")
    void issuesASessionBoundToTheTerminalForACorrectPin() {
        CORE_SERVICE.stubFor(post(urlEqualTo("/internal/auth/atm/pin-verifications"))
                .withRequestBody(equalToJson("{\"cardNumber\":\"card-1\",\"pin\":\"1234\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"customerId\":\"customer-1\"}")));

        String sessionToken = given()
                .contentType("application/json")
                .body("{\"cardNumber\":\"card-1\",\"pin\":\"1234\"}")
                .when()
                .post("/pin-verifications")
                .then()
                .statusCode(200)
                .body("sessionToken", notNullValue())
                .extract()
                .path("sessionToken");

        CallerContext callerContext = tokenAdapter.resolve(sessionToken);
        assertEquals("customer-1", callerContext.customerId());
        assertEquals(Channel.ATM, callerContext.channel());
        assertEquals("atm-terminal-001", callerContext.terminalId().orElseThrow());
    }

    @Test
    @DisplayName("rejects an incorrect pin with no session issued")
    void rejectsAnIncorrectPinWithNoSessionIssued() {
        CORE_SERVICE.stubFor(post(urlEqualTo("/internal/auth/atm/pin-verifications"))
                .willReturn(aResponse().withStatus(401)));

        given()
                .contentType("application/json")
                .body("{\"cardNumber\":\"card-1\",\"pin\":\"9999\"}")
                .when()
                .post("/pin-verifications")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("rejects an unknown card identically to an incorrect pin")
    void rejectsAnUnknownCardIdenticallyToAnIncorrectPin() {
        CORE_SERVICE.stubFor(post(urlEqualTo("/internal/auth/atm/pin-verifications"))
                .willReturn(aResponse().withStatus(401)));

        given()
                .contentType("application/json")
                .body("{\"cardNumber\":\"unknown-card\",\"pin\":\"1234\"}")
                .when()
                .post("/pin-verifications")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("places the pin only in the request body, and calls core-service over tls")
    void placesThePinOnlyInTheRequestBodyAndCallsCoreServiceOverTls() {
        CORE_SERVICE.stubFor(post(urlEqualTo("/internal/auth/atm/pin-verifications"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"customerId\":\"customer-1\"}")));

        given()
                .contentType("application/json")
                .body("{\"cardNumber\":\"card-1\",\"pin\":\"1234\"}")
                .when()
                .post("/pin-verifications")
                .then()
                .statusCode(200);

        CORE_SERVICE.verify(postRequestedFor(urlEqualTo("/internal/auth/atm/pin-verifications")));
        List<ServeEvent> events = CORE_SERVICE.getAllServeEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0).getRequest().getAbsoluteUrl().contains(":" + CORE_SERVICE.httpsPort()));
        assertTrue(events.get(0).getRequest().getBodyAsString().contains("1234"));
    }
}
