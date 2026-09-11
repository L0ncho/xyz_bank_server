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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("The JwtCallerContextAdapter")
class JwtCallerContextAdapterTest {

    /*
     * Cases:
     * 1. Issues a token whose claims carry the customer id, channel, and the channel's exact scope set
     * 2. Issues a token with no terminal id claim when none is supplied
     * 3. Issues a token with a terminal id claim when one is supplied
     */

    private static final String SECRET = "unit-test-signing-secret-unit-test-signing-secret";

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

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private Set<String> scopesOf(Claims claims) {
        String scope = claims.get("scope", String.class);
        return Stream.of(scope.split(" ")).collect(Collectors.toSet());
    }
}
