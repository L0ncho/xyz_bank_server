package cl.duoc.xyzbank.bffmobile.shared.e2e;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bff-mobile's TLS connector")
class BffMobileTlsE2ETest {

    /*
     * Cases:
     * 1. Serves a request over HTTPS
     * 2. Refuses a plain HTTP connection on the same port
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
    @DisplayName("serves the health endpoint over HTTPS")
    void servesOverHttps() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("refuses a plain HTTP connection on the same port")
    void refusesPlainHttp() throws IOException {
        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(2000);
            socket.getOutputStream().write("GET /actuator/health HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
            socket.getOutputStream().flush();
            String statusLine;
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                statusLine = reader.readLine();
            } catch (IOException connectionRejected) {
                statusLine = null;
            }
            assertTrue(
                    statusLine == null || !statusLine.contains("200"),
                    "expected the plain HTTP request to be rejected, but got: " + statusLine);
        }
    }
}
