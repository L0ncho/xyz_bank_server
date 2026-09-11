package cl.duoc.xyzbank.sharedsecurity.callercontext.unit;

import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The JwtCallerContextAdapter")
class JwtCallerContextAdapterTest {

    /*
     * Cases:
     * 1. Issues a token whose claims carry the customer id, channel, and the channel's exact scope set
     * 2. Issues a token with no terminal id claim when none is supplied
     * 3. Issues a token with a terminal id claim when one is supplied
     * 4. Caps an ATM token's lifetime at 120 seconds after issuance
     * 5. Issues a web/mobile token with a short-lived default expiry longer than 120 seconds
     */

    private static final String SECRET = "unit-test-signing-secret-unit-test-signing-secret";
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final JwtCallerContextAdapter adapter = new JwtCallerContextAdapter(SECRET);

    @Test
    @DisplayName("issues a token carrying the customer id, channel, and the channel's exact scope set")
    void issuesTokenWithCustomerIdChannelAndScopes() {
        String token = adapter.issue("customer-1", Channel.WEB, null);

        Claims claims = parseClaims(token);

        assertEquals("customer-1", claims.getSubject());
        assertEquals("WEB", claims.get("channel", String.class));
        assertEquals(Channel.WEB.scopes(), scopesOf(claims));
    }

    @Test
    @DisplayName("issues a token with no terminal id claim when none is supplied")
    void issuesTokenWithNoTerminalIdWhenNoneSupplied() {
        String token = adapter.issue("customer-1", Channel.WEB, null);

        Claims claims = parseClaims(token);

        assertNull(claims.get("terminalId"));
    }

    @Test
    @DisplayName("issues a token with a terminal id claim when one is supplied")
    void issuesTokenWithTerminalIdWhenSupplied() {
        String token = adapter.issue("customer-3", Channel.ATM, "terminal-9");

        Claims claims = parseClaims(token);

        assertEquals("terminal-9", claims.get("terminalId", String.class));
    }

    @Test
    @DisplayName("caps an atm token's lifetime at 120 seconds after issuance")
    void capsAtmTokenLifetimeAt120Seconds() {
        JwtCallerContextAdapter fixedClockAdapter = new JwtCallerContextAdapter(
                SECRET, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

        String token = fixedClockAdapter.issue("customer-3", Channel.ATM, "terminal-9");
        Claims claims = parseClaims(token, FIXED_NOW);

        assertEquals(FIXED_NOW.plus(Duration.ofSeconds(120)), claims.getExpiration().toInstant());
    }

    @Test
    @DisplayName("issues a web token with a short-lived default expiry longer than 120 seconds")
    void issuesWebTokenWithShortLivedDefaultExpiry() {
        JwtCallerContextAdapter fixedClockAdapter = new JwtCallerContextAdapter(
                SECRET, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));

        String token = fixedClockAdapter.issue("customer-1", Channel.WEB, null);
        Claims claims = parseClaims(token, FIXED_NOW);

        Duration lifetime = Duration.between(FIXED_NOW, claims.getExpiration().toInstant());
        assertTrue(lifetime.compareTo(Duration.ofSeconds(120)) > 0);
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private Claims parseClaims(String token, Instant asOf) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(asOf))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Set<String> scopesOf(Claims claims) {
        String scope = claims.get("scope", String.class);
        return Stream.of(scope.split(" ")).collect(Collectors.toSet());
    }
}
