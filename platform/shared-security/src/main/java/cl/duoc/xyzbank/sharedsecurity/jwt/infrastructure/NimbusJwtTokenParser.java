package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtAuthenticationException;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.ports.JwtTokenParser;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.ports.ParsedJwtClaims;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class NimbusJwtTokenParser implements JwtTokenParser {

    private final byte[] secret;
    private final String issuer;

    public NimbusJwtTokenParser(String secret, String issuer) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
    }

    @Override
    public ParsedJwtClaims parse(String compactJwt) {
        if (compactJwt == null || compactJwt.isBlank()) {
            throw JwtAuthenticationException.invalid("JWT is required");
        }
        try {
            SignedJWT signedJwt = SignedJWT.parse(compactJwt);
            if (!signedJwt.verify(new MACVerifier(secret))) {
                throw JwtAuthenticationException.invalid("JWT signature is invalid");
            }
            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            Instant now = Instant.now();
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.toInstant().isBefore(now)) {
                throw JwtAuthenticationException.invalid("JWT is expired");
            }
            if (issuer != null && !issuer.equals(claims.getIssuer())) {
                throw JwtAuthenticationException.invalid("JWT issuer is invalid");
            }
            return new ParsedJwtClaims(
                    claims.getSubject(),
                    claims.getStringClaim("channel"),
                    claims.getStringClaim("terminalId"),
                    rolesFrom(claims));
        } catch (JwtAuthenticationException exception) {
            throw exception;
        } catch (ParseException | JOSEException exception) {
            throw JwtAuthenticationException.invalid("JWT is invalid");
        }
    }

    private static List<String> rolesFrom(JWTClaimsSet claims) throws ParseException {
        List<String> roles = claims.getStringListClaim("roles");
        if (roles != null) {
            return roles;
        }
        String role = claims.getStringClaim("roles");
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(role);
    }
}
