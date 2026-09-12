package cl.duoc.xyzbank.bffweb.auth.e2e;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("bff-web's session refresh endpoint")
class SessionRefreshE2ETest {

    /*
     * Cases:
     * 1. A successful rotation sets a new session cookie and a new refresh_token cookie,
     *    both carrying HttpOnly/Secure/SameSite
     * 2. A rejected/reused refresh token clears both cookies and requires re-login
     * 3. No refresh_token cookie at all is rejected without calling core-service
     * 4. A missing/mismatched CSRF token is rejected before core-service is ever called
     * 5. A successful rotation also issues a (non-HttpOnly) CSRF cookie
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
    @DisplayName("rotates the session on a valid refresh token")
    void rotatesTheSessionOnAValidRefreshToken() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/web/refresh-tokens"))
                .withRequestBody(equalToJson("{\"customerId\":null,\"refreshToken\":\"old-refresh-token\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"customerId\":\"customer-42\",\"refreshToken\":\"new-refresh-token\","
                                + "\"expiry\":\"2099-01-01T00:00:00Z\"}")));

        Response response = given()
                .cookie("refresh_token", "old-refresh-token")
                .cookie("XSRF-TOKEN", "csrf-token-1")
                .header("X-XSRF-TOKEN", "csrf-token-1")
                .when()
                .post("/session/refresh");

        response.then().statusCode(204);
        List<String> setCookieHeaders = response.getHeaders().getValues("Set-Cookie");
        String sessionCookie = setCookieHeaders.stream()
                .filter(header -> header.startsWith("session="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no session cookie set"));
        String refreshCookie = setCookieHeaders.stream()
                .filter(header -> header.startsWith("refresh_token="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no refresh_token cookie set"));

        assertTrue(sessionCookie.contains("HttpOnly") && sessionCookie.contains("Secure")
                && sessionCookie.contains("SameSite"));
        assertTrue(refreshCookie.contains("new-refresh-token"));
        assertTrue(refreshCookie.contains("HttpOnly") && refreshCookie.contains("Secure")
                && refreshCookie.contains("SameSite"));

        String csrfCookie = setCookieHeaders.stream()
                .filter(header -> header.startsWith("XSRF-TOKEN="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no XSRF-TOKEN cookie set"));
        assertTrue(!csrfCookie.contains("HttpOnly"), "the CSRF cookie must not be HttpOnly: " + csrfCookie);
    }

    @Test
    @DisplayName("rejects a request with a missing or mismatched CSRF token before calling core-service")
    void rejectsARequestWithAMissingOrMismatchedCsrfToken() {
        given()
                .cookie("refresh_token", "old-refresh-token")
                .when()
                .post("/session/refresh")
                .then()
                .statusCode(403);

        given()
                .cookie("refresh_token", "old-refresh-token")
                .cookie("XSRF-TOKEN", "csrf-token-a")
                .header("X-XSRF-TOKEN", "csrf-token-b")
                .when()
                .post("/session/refresh")
                .then()
                .statusCode(403);

        CORE_SERVICE.verify(
                0, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                        urlPathEqualTo("/internal/auth/web/refresh-tokens")));
    }

    @Test
    @DisplayName("clears both cookies and requires re-login when the refresh token is rejected")
    void clearsCookiesWhenTheRefreshTokenIsRejected() {
        CORE_SERVICE.stubFor(post(urlPathEqualTo("/internal/auth/web/refresh-tokens"))
                .withRequestBody(equalToJson("{\"customerId\":null,\"refreshToken\":\"reused-token\"}"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"detail\":\"Refresh token was already used\"}")));

        Response response = given()
                .cookie("refresh_token", "reused-token")
                .cookie("XSRF-TOKEN", "csrf-token-2")
                .header("X-XSRF-TOKEN", "csrf-token-2")
                .when()
                .post("/session/refresh");

        response.then().statusCode(401);
        List<String> setCookieHeaders = response.getHeaders().getValues("Set-Cookie");
        assertTrue(setCookieHeaders.stream().anyMatch(header -> header.startsWith("session=") && header.contains("Max-Age=0")));
        assertTrue(setCookieHeaders.stream()
                .anyMatch(header -> header.startsWith("refresh_token=") && header.contains("Max-Age=0")));
    }

    @Test
    @DisplayName("rejects a request with no refresh_token cookie without calling core-service")
    void rejectsARequestWithNoRefreshTokenCookie() {
        given()
                .cookie("XSRF-TOKEN", "csrf-token-3")
                .header("X-XSRF-TOKEN", "csrf-token-3")
                .when()
                .post("/session/refresh")
                .then()
                .statusCode(401);

        CORE_SERVICE.verify(
                0, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(
                        urlPathEqualTo("/internal/auth/web/refresh-tokens")));
    }
}
