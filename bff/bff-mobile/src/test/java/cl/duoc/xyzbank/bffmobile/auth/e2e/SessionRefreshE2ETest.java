package cl.duoc.xyzbank.bffmobile.auth.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bff-mobile's session refresh endpoint")
class SessionRefreshE2ETest {

    /*
     * Cases:
     * 1. A successful rotation returns a new session token and a new refresh token
     * 2. A device-id mismatch (core-service's 404) is forwarded as-is
     * 3. A revoked device (core-service's 409) is forwarded as-is
     */

    private static final WireMockServer CORE_SERVICE = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void startCoreService() {
        CORE_SERVICE.start();
    }

    @AfterAll
    static void stopCoreService() {
        CORE_SERVICE.stop();
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
        RestAssured.baseURI = "https://localhost";
        RestAssured.useRelaxedHTTPSValidation();
        CORE_SERVICE.resetAll();
    }

    @Test
    @DisplayName("rotates the session and returns a new session token and refresh token")
    void rotatesTheSessionAndReturnsANewSessionTokenAndRefreshToken() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/mobile/devices/device-1/refresh-tokens"))
                .withRequestBody(equalToJson("{\"customerId\":null,\"refreshToken\":\"old-refresh-token\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"customerId\":\"customer-42\",\"refreshToken\":\"new-refresh-token\","
                                + "\"expiry\":\"2099-01-01T00:00:00Z\"}")));

        given()
                .contentType("application/json")
                .body("{\"deviceId\":\"device-1\",\"refreshToken\":\"old-refresh-token\"}")
                .when()
                .post("/session/refresh")
                .then()
                .statusCode(200)
                .body("sessionToken", org.hamcrest.Matchers.notNullValue())
                .body("refreshToken", org.hamcrest.Matchers.equalTo("new-refresh-token"));
    }

    @Test
    @DisplayName("forwards core-service's not-found response for a device-id mismatch")
    void forwardsNotFoundForADeviceIdMismatch() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/mobile/devices/device-1/refresh-tokens"))
                .withRequestBody(equalToJson("{\"customerId\":null,\"refreshToken\":\"wrong-device-token\"}"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"detail\":\"Refresh token not found\"}")));

        given()
                .contentType("application/json")
                .body("{\"deviceId\":\"device-1\",\"refreshToken\":\"wrong-device-token\"}")
                .when()
                .post("/session/refresh")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("forwards core-service's conflict response for a revoked device")
    void forwardsConflictForARevokedDevice() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/mobile/devices/device-1/refresh-tokens"))
                .withRequestBody(equalToJson("{\"customerId\":null,\"refreshToken\":\"revoked-device-token\"}"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"detail\":\"Device has been revoked\"}")));

        given()
                .contentType("application/json")
                .body("{\"deviceId\":\"device-1\",\"refreshToken\":\"revoked-device-token\"}")
                .when()
                .post("/session/refresh")
                .then()
                .statusCode(409);
    }
}
