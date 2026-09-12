package cl.duoc.xyzbank.coreservice.auth.e2e;

import cl.duoc.xyzbank.coredomain.accounts.domain.entities.Customer;
import cl.duoc.xyzbank.coredomain.accounts.domain.repositories.CustomerRepository;
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
@DisplayName("The web refresh token endpoint")
class WebRefreshTokenControllerE2ETest extends AbstractPostgresIT {

    /*
     * Cases:
     * 1. First-time issuance (no prior token) returns 200 with a fresh refresh token
     * 2. Successful rotation returns 200 with a new refresh token
     * 3. Reusing an already-rotated refresh token returns 409
     */

    @Autowired
    private CustomerRepository customerRepository;

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.requestSpecification =
                given().header("X-Service-Credential", "dev-service-credential-web");
    }

    private Id newCustomer() {
        Id customerId = Id.generate();
        customerRepository.save(Customer.create(customerId, "Test Customer", "test.customer@xyzbank.cl"));
        return customerId;
    }

    @Test
    @DisplayName("first-time issuance returns 200 with a fresh refresh token")
    void firstTimeIssuanceReturns200WithFreshToken() {
        given()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/web/refresh-tokens")
                .then()
                .statusCode(200)
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("successful rotation returns 200 with a new refresh token")
    void successfulRotationReturns200WithNewToken() {
        Response first = given()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/web/refresh-tokens");
        String firstToken = first.jsonPath().getString("refreshToken");

        given()
                .contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/web/refresh-tokens")
                .then()
                .statusCode(200)
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("reusing an already-rotated refresh token returns 409")
    void reusingAlreadyRotatedTokenReturns409() {
        Response first = given()
                .contentType("application/json")
                .body("{\"customerId\":\"" + newCustomer().getValue() + "\"}")
                .when()
                .post("/internal/auth/web/refresh-tokens");
        String firstToken = first.jsonPath().getString("refreshToken");
        given().contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/web/refresh-tokens");

        given()
                .contentType("application/json")
                .body("{\"refreshToken\":\"" + firstToken + "\"}")
                .when()
                .post("/internal/auth/web/refresh-tokens")
                .then()
                .statusCode(409);
    }
}
