package cl.duoc.xyzbank.authorizationserver.e2e;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("The authorization server")
class AuthorizationServerE2ETest {

    /*
     * 1. Publishes a JWKS document with a signing key — done
     * 2. A registered client obtains an access token that is a JWT — done
     * 3. That JWT signature verifies against the published JWKS — done
     * 4. An unregistered client is rejected at the token endpoint — done
     * 5. The access token carries the authorization server issuer — done
     * 6. A registered client completes authorization_code + PKCE — done
     */

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("publishes a JWKS document with a signing key")
    void publishesAJwksWithASigningKey() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty());
    }

    @Test
    @DisplayName("issues a JWT access token to a registered client")
    void issuesAJwtAccessTokenToARegisteredClient() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .header(HttpHeaders.AUTHORIZATION, basicCredentials("xyz-bank-web-dev", "xyz-bank-web-dev-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andReturn();

        String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");

        assertEquals(3, accessToken.split("\\.").length);
    }

    @Test
    @DisplayName("signs the access token with the published JWKS")
    void signsTheAccessTokenWithThePublishedJwks() throws Exception {
        String jwks = mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = issuedAccessToken();

        SignedJWT signedJwt = SignedJWT.parse(accessToken);
        RSAKey signingKey = (RSAKey) JWKSet.parse(jwks).getKeyByKeyId(signedJwt.getHeader().getKeyID());

        assertTrue(signedJwt.verify(new RSASSAVerifier(signingKey)));
    }

    @Test
    @DisplayName("rejects an unregistered client at the token endpoint")
    void rejectsAnUnregisteredClientAtTheTokenEndpoint() throws Exception {
        mockMvc.perform(post("/oauth2/token")
                        .header(HttpHeaders.AUTHORIZATION, basicCredentials("unknown-client", "unknown-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    @DisplayName("issues an access token with the authorization server issuer")
    void issuesAnAccessTokenWithTheAuthorizationServerIssuer() throws Exception {
        SignedJWT signedJwt = SignedJWT.parse(issuedAccessToken());

        assertEquals("http://localhost:9000", signedJwt.getJWTClaimsSet().getIssuer());
    }

    @Test
    @DisplayName("completes authorization code with PKCE for a registered client")
    void completesAuthorizationCodeWithPkceForARegisteredClient() throws Exception {
        String redirectUri = "https://localhost:8081/login/oauth2/code/oidc";
        String codeVerifier = codeVerifier();
        String codeChallenge = codeChallenge(codeVerifier);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/oauth2/authorize")
                        .session(session)
                        .accept(MediaType.TEXT_HTML)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", "xyz-bank-web-dev")
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("scope", "openid")
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        MvcResult afterLogin = mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", "demo")
                        .param("password", "demo")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MvcResult authorized = mockMvc.perform(get(afterLogin.getResponse().getRedirectedUrl()).session(session))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String code = UriComponentsBuilder.fromUriString(authorized.getResponse().getRedirectedUrl())
                .build()
                .getQueryParams()
                .getFirst("code");

        MvcResult token = mockMvc.perform(post("/oauth2/token")
                        .header(HttpHeaders.AUTHORIZATION, basicCredentials("xyz-bank-web-dev", "xyz-bank-web-dev-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "authorization_code")
                        .param("code", code)
                        .param("redirect_uri", redirectUri)
                        .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andReturn();

        String accessToken = JsonPath.read(token.getResponse().getContentAsString(), "$.access_token");

        assertEquals(3, accessToken.split("\\.").length);
    }

    private String issuedAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .header(HttpHeaders.AUTHORIZATION, basicCredentials("xyz-bank-web-dev", "xyz-bank-web-dev-secret"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }

    private static String codeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String codeChallenge(String codeVerifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String basicCredentials(String clientId, String clientSecret) {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
