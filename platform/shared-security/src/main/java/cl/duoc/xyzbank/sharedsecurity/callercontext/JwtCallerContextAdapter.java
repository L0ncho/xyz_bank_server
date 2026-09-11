package cl.duoc.xyzbank.sharedsecurity.callercontext;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

public final class JwtCallerContextAdapter {

    private static final Duration ATM_SESSION_TTL = Duration.ofSeconds(120);
    private static final Duration DEFAULT_SESSION_TTL = Duration.ofMinutes(15);

    private final SecretKey key;
    private final Clock clock;

    public JwtCallerContextAdapter(String secret) {
        this(secret, Clock.systemUTC());
    }

    public JwtCallerContextAdapter(String secret, Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String issue(String customerId, Channel channel, String terminalId) {
        Instant now = clock.instant();
        JwtBuilder builder = Jwts.builder()
                .subject(customerId)
                .claim("channel", channel.name())
                .claim("scope", String.join(" ", channel.scopes()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiryFor(channel))))
                .signWith(key);
        if (terminalId != null) {
            builder.claim("terminalId", terminalId);
        }
        return builder.compact();
    }

    public CallerContext resolve(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Channel channel = Channel.valueOf(claims.get("channel", String.class));
        String customerId = claims.getSubject();
        String terminalId = claims.get("terminalId", String.class);
        return new ResolvedCallerContext(customerId, channel, channel.scopes(), terminalId);
    }

    private Duration expiryFor(Channel channel) {
        return channel == Channel.ATM ? ATM_SESSION_TTL : DEFAULT_SESSION_TTL;
    }

    private record ResolvedCallerContext(String customerId, Channel channel, Set<String> scopes, String rawTerminalId)
            implements CallerContext {

        @Override
        public Optional<String> terminalId() {
            return Optional.ofNullable(rawTerminalId);
        }
    }
}
