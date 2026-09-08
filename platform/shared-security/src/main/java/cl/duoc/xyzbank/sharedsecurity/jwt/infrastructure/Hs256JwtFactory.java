package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class Hs256JwtFactory {

    public static final String DEFAULT_SECRET = "dev-only-change-me-use-32-chars-min";
    public static final String DEFAULT_ISSUER = "xyz-bank";

    private Hs256JwtFactory() {
    }

    public static String devToken(String subject, String channel) {
        return issue(DEFAULT_SECRET, DEFAULT_ISSUER, Duration.ofHours(1), subject, channel, null);
    }

    public static String devToken(String subject, String channel, String terminalId) {
        return issue(DEFAULT_SECRET, DEFAULT_ISSUER, Duration.ofHours(1), subject, channel, terminalId);
    }

    public static String issue(
            String secret,
            String issuer,
            Duration timeToLive,
            String subject,
            String channel,
            String terminalId) {
        return issue(secret, issuer, timeToLive, subject, channel, terminalId, rolesForChannel(channel));
    }

    public static String issue(
            String secret,
            String issuer,
            Duration timeToLive,
            String subject,
            String channel,
            String terminalId,
            List<String> roles) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(timeToLive)))
                .claim("channel", channel);
        if (terminalId != null && !terminalId.isBlank()) {
            claims.claim("terminalId", terminalId);
        }
        if (roles != null && !roles.isEmpty()) {
            claims.claim("roles", roles);
        }
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        try {
            signedJwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
        return signedJwt.serialize();
    }

    public static List<String> rolesForChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return List.of();
        }
        return List.of("ROLE_" + channel.trim().toUpperCase());
    }
}
