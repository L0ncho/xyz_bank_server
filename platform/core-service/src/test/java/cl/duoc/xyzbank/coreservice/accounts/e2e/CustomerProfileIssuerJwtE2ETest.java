package cl.duoc.xyzbank.coreservice.accounts.e2e;

import cl.duoc.xyzbank.coredomain.accounts.domain.entities.Customer;
import cl.duoc.xyzbank.coredomain.accounts.domain.repositories.CustomerRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.testsupport.AbstractPostgresIT;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The customer profile and an authorization-server access token")
class CustomerProfileIssuerJwtE2ETest extends AbstractPostgresIT {

    /*
     * 1. A profile request with a service credential and no bearer is rejected (done)
     * 2. A profile request with a JWT signed by another key is rejected (done)
     * 3. A profile request with the issuer's JWT and a service credential succeeds (done)
     */

    private static final String ISSUER = "http://localhost:9000";
    private static final String KEY_ID = "issuer-test";
    private static final KeyPair ISSUER_KEY = issuerKey();
    private static final HttpServer JWKS_SERVER = jwksServer();

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("rejects a profile request that has no bearer token")
    void rejectsAProfileRequestThatHasNoBearerToken() {
        given()
                .header("X-Service-Credential", "dev-service-credential-web")
                .when().get("/internal/customers/{customerId}", Id.generate().getValue())
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("rejects a profile request whose jwt was signed by another key")
    void rejectsAProfileRequestWhoseJwtWasSignedByAnotherKey() throws Exception {
        given()
                .header("X-Service-Credential", "dev-service-credential-web")
                .header("Authorization", "Bearer " + tokenSignedByAnotherKey())
                .when().get("/internal/customers/{customerId}", Id.generate().getValue())
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("accepts a profile request with the issuer jwt and a service credential")
    void acceptsAProfileRequestWithTheIssuerJwtAndAServiceCredential() {
        Id id = Id.generate();
        customerRepository.save(Customer.create(id, "Jane Doe", "jane.doe@xyzbank.cl"));

        given()
                .header("X-Service-Credential", "dev-service-credential-web")
                .header("Authorization", "Bearer " + issuerToken())
                .when().get("/internal/customers/{customerId}", id.getValue())
                .then()
                .statusCode(200)
                .body("id", equalTo(id.getValue()))
                .body("fullName", equalTo("Jane Doe"))
                .body("email", equalTo("jane.doe@xyzbank.cl"));
    }

    private static String issuerToken() {
        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .issuer(ISSUER)
                .subject("service")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(ISSUER_KEY.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static String tokenSignedByAnotherKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return Jwts.builder()
                .issuer(ISSUER)
                .subject("customer")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(generator.generateKeyPair().getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static KeyPair issuerKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServer jwksServer() {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) ISSUER_KEY.getPublic();
            String jwks = """
                    {"keys":[{"kty":"RSA","use":"sig","alg":"RS256","kid":"%s","n":"%s","e":"%s"}]}
                    """.formatted(
                    KEY_ID,
                    unsignedBase64Url(publicKey.getModulus()),
                    unsignedBase64Url(publicKey.getPublicExponent())).trim();
            HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
            server.createContext("/oauth2/jwks", exchange -> {
                byte[] body = jwks.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String unsignedBase64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            bytes = trimmed;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
