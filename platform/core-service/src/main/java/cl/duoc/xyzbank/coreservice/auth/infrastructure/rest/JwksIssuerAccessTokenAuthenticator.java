package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Set;

public final class JwksIssuerAccessTokenAuthenticator implements IssuerAccessTokenAuthenticator {

    private static final String expectedIssuer = "http://localhost:9000";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DefaultJWTProcessor<SecurityContext> processor;

    public JwksIssuerAccessTokenAuthenticator(String jwkSetUri) {
        JWKSource<SecurityContext> jwkSource = jwkSource(jwkSetUri);
        DefaultJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource));
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().issuer(expectedIssuer).build(),
                Set.of("iss", "exp")));
        this.processor = jwtProcessor;
    }

    @Override
    public Decision authenticate(String bearerToken) {
        if (!isRs256FromIssuer(bearerToken)) {
            return Decision.NOT_AN_ISSUER_TOKEN;
        }
        try {
            processor.process(bearerToken, null);
            return Decision.ACCEPTED;
        } catch (ParseException | BadJOSEException | JOSEException exception) {
            return Decision.REJECTED;
        }
    }

    private JWKSource<SecurityContext> jwkSource(String jwkSetUri) {
        try {
            return JWKSourceBuilder.create(URI.create(jwkSetUri).toURL()).build();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Issuer JWKS URL is not valid");
        }
    }

    private boolean isRs256FromIssuer(String bearerToken) {
        String[] parts = bearerToken.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            JsonNode header = objectMapper.readTree(decode(parts[0]));
            JsonNode payload = objectMapper.readTree(decode(parts[1]));
            return "RS256".equals(header.path("alg").asText())
                    && expectedIssuer.equals(payload.path("iss").asText());
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] decode(String part) {
        int remainder = part.length() % 4;
        String padded = remainder == 0 ? part : part + "====".substring(remainder);
        return Base64.getUrlDecoder().decode(padded.getBytes(StandardCharsets.US_ASCII));
    }
}
