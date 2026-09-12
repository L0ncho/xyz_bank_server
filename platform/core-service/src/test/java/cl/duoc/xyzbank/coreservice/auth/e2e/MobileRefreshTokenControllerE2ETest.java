package cl.duoc.xyzbank.coreservice.auth.e2e;

import cl.duoc.xyzbank.coredomain.accounts.domain.entities.Customer;
import cl.duoc.xyzbank.coredomain.accounts.domain.repositories.CustomerRepository;
import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.DeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.testsupport.AbstractPostgresIT;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The mobile refresh token endpoint")
class MobileRefreshTokenControllerE2ETest extends AbstractPostgresIT {

    /*
     * Cases:
     * 1. First-time issuance registers the device and returns 200 with a fresh refresh token
     * 2. Successful rotation returns 200 with a new refresh token
     * 3. Reusing an already-rotated refresh token returns 409
     * 4. Rotation for a revoked device returns 409
     */

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DeviceRegistrationRepository deviceRegistrationRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
    }

    private io.restassured.specification.RequestSpecification asService() {
        return given().header("X-Service-Credential", "dev-service-credential-mobile");
    }

    private Id newCustomer() {
        Id customerId = Id.generate();
        customerRepository.save(Customer.create(customerId, "Test Customer", "test.customer@xyzbank.cl"));
        return customerId;
    }

    @Test
    @DisplayName("first-time issuance registers the device and returns 200 with a fresh token")
    void firstTimeIssuanceReturns200WithFreshToken() {
        String deviceId = Id.generate().getValue();

        asService()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId + "/refresh-tokens")
                .then()
                .statusCode(200)
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("successful rotation returns 200 with a new refresh token")
    void successfulRotationReturns200WithNewToken() {
        String deviceId = Id.generate().getValue();
        Response first = asService()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId + "/refresh-tokens");
        String firstToken = first.jsonPath().getString("refreshToken");

        asService()
                .contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId + "/refresh-tokens")
                .then()
                .statusCode(200)
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("reusing an already-rotated refresh token returns 409")
    void reusingAlreadyRotatedTokenReturns409() {
        String deviceId = Id.generate().getValue();
        Response first = asService()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId + "/refresh-tokens");
        String firstToken = first.jsonPath().getString("refreshToken");
        asService().contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId + "/refresh-tokens");

        asService()
                .contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId + "/refresh-tokens")
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("rotation for a revoked device returns 409")
    void rotationForRevokedDeviceReturns409() {
        Id deviceId = Id.generate();
        Response first = asService()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId.getValue() + "/refresh-tokens");
        String firstToken = first.jsonPath().getString("refreshToken");
        DeviceRegistration device = deviceRegistrationRepository.findByDeviceId(deviceId).orElseThrow();
        device.revoke();
        deviceRegistrationRepository.save(device);

        asService()
                .contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId.getValue() + "/refresh-tokens")
                .then()
                .statusCode(409);
    }
}
