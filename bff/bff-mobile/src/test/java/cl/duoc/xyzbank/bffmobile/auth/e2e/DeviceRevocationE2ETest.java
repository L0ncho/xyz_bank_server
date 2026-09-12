package cl.duoc.xyzbank.bffmobile.auth.e2e;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bff-mobile's device revocation endpoint")
class DeviceRevocationE2ETest {

    /*
     * Cases:
     * 1. A successful revocation is forwarded to core-service and returns 204
     * 2. A second device belonging to the same customer is unaffected by another's revocation
     * 3. A device may not revoke a device other than itself
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

    @Autowired
    private JwtCallerContextAdapter tokenAdapter;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.useRelaxedHTTPSValidation();
        CORE_SERVICE.resetAll();
    }

    private RequestSpecification asDevice(String deviceId) {
        return given()
                .header("Authorization", "Bearer " + tokenAdapter.issue("customer-1", Channel.MOBILE, deviceId))
                .header("X-Device-Id", deviceId);
    }

    @Test
    @DisplayName("forwards a successful revocation to core-service")
    void forwardsASuccessfulRevocationToCoreService() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/mobile/devices/device-1/revocations"))
                .willReturn(aResponse().withStatus(204)));

        asDevice("device-1")
                .when()
                .post("/devices/{deviceId}/revocations", "device-1")
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("leaves a second device belonging to the same customer unaffected")
    void leavesASecondDeviceBelongingToTheSameCustomerUnaffected() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/mobile/devices/device-1/revocations"))
                .willReturn(aResponse().withStatus(204)));
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/mobile/devices/device-2/revocations"))
                .willReturn(aResponse().withStatus(204)));

        asDevice("device-1")
                .when()
                .post("/devices/{deviceId}/revocations", "device-1")
                .then()
                .statusCode(204);

        asDevice("device-2")
                .when()
                .post("/devices/{deviceId}/revocations", "device-2")
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("rejects a device attempting to revoke a different device")
    void rejectsADeviceAttemptingToRevokeADifferentDevice() {
        asDevice("device-1")
                .when()
                .post("/devices/{deviceId}/revocations", "device-2")
                .then()
                .statusCode(403)
                .contentType("application/problem+json");
    }
}
