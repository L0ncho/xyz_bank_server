package cl.duoc.xyzbank.bffweb.auth.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bff-web's OIDC login endpoints")
class OidcLoginReachabilityE2ETest {

    /*
     * Cases:
     * 1. The login-initiation endpoint redirects to the configured authorization endpoint
     * 2. The callback endpoint exists (does not 404) even without a matching prior request
     * 3. Every other endpoint remains reachable without authentication (the security filter
     *    chain permits all requests; only the OIDC handshake itself is handled specially)
     */

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "https://localhost";
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @DisplayName("redirects to the configured authorization endpoint")
    void redirectsToTheConfiguredAuthorizationEndpoint() {
        given()
                .redirects().follow(false)
                .when().get("/oauth2/authorization/oidc")
                .then()
                .statusCode(302)
                .header("Location", startsWith("http://localhost:9999/mock-oidc/authorize"))
                .header("Location", containsString("code_challenge"));
    }

    @Test
    @DisplayName("the callback endpoint does not 404")
    void theCallbackEndpointDoesNotReturnNotFound() {
        int status = given()
                .redirects().follow(false)
                .when().get("/login/oauth2/code/oidc")
                .statusCode();

        org.junit.jupiter.api.Assertions.assertTrue(status != 404, "expected the callback route to exist, got " + status);
    }

    @Test
    @DisplayName("every other endpoint stays reachable without authentication")
    void everyOtherEndpointStaysReachableWithoutAuthentication() {
        given()
                .when().get("/actuator/health")
                .then()
                .statusCode(200);
    }
}
