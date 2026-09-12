package cl.duoc.xyzbank.coreservice.auth.e2e;

import cl.duoc.xyzbank.coredomain.accounts.domain.entities.Customer;
import cl.duoc.xyzbank.coredomain.accounts.domain.repositories.CustomerRepository;
import cl.duoc.xyzbank.coredomain.auth.domain.entities.DeviceRegistration;
import cl.duoc.xyzbank.coredomain.auth.domain.repositories.DeviceRegistrationRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.testsupport.AbstractPostgresIT;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The device revocation endpoint")
class DeviceRevocationControllerE2ETest extends AbstractPostgresIT {

    /*
     * Cases:
     * 1. Successful revocation returns 204 and marks the device revoked
     * 2. Revoking an unknown device returns 404
     */

    @Autowired
    private DeviceRegistrationRepository deviceRegistrationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.requestSpecification =
                given().header("X-Service-Credential", "dev-service-credential-mobile");
    }

    private Id newCustomer() {
        Id customerId = Id.generate();
        customerRepository.save(Customer.create(customerId, "Test Customer", "test.customer@xyzbank.cl"));
        return customerId;
    }

    @Test
    @DisplayName("successful revocation returns 204 and marks the device revoked")
    void successfulRevocationReturns204AndMarksDeviceRevoked() {
        Id deviceId = Id.generate();
        deviceRegistrationRepository.save(DeviceRegistration.register(deviceId, newCustomer()));

        given()
                .when()
                .post("/internal/auth/mobile/devices/" + deviceId.getValue() + "/revocations")
                .then()
                .statusCode(204);

        assertTrue(deviceRegistrationRepository.findByDeviceId(deviceId).orElseThrow().isRevoked());
    }

    @Test
    @DisplayName("revoking an unknown device returns 404")
    void revokingUnknownDeviceReturns404() {
        given()
                .when()
                .post("/internal/auth/mobile/devices/" + Id.generate().getValue() + "/revocations")
                .then()
                .statusCode(404);
    }
}
