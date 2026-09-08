package cl.duoc.xyzbank.sharedsecurity.jwt.unit;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.AuthenticatedCaller;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtAuthenticationException;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtCallerAuthenticator;
import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.Hs256JwtFactory;
import cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure.NimbusJwtTokenParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The JwtCallerAuthenticator")
class JwtCallerAuthenticatorTest {

    private final JwtCallerAuthenticator authenticator = new JwtCallerAuthenticator(
            new NimbusJwtTokenParser(Hs256JwtFactory.DEFAULT_SECRET, Hs256JwtFactory.DEFAULT_ISSUER));

    @Test
    @DisplayName("resolves a web caller and ROLE_WEB from a valid JWT")
    void resolvesAWebCallerAndRoleWebFromAValidJwt() {
        String token = Hs256JwtFactory.devToken("customer-1", "web");

        AuthenticatedCaller authenticated = authenticator.authenticate(token);

        assertEquals("customer-1", authenticated.callerContext().customerId());
        assertEquals(Channel.WEB, authenticated.callerContext().channel());
        assertEquals(Optional.empty(), authenticated.callerContext().terminalId());
        assertEquals(List.of("ROLE_WEB"), authenticated.roles());
    }

    @Test
    @DisplayName("resolves an atm caller including its terminal id and ROLE_ATM")
    void resolvesAnAtmCallerIncludingItsTerminalIdAndRoleAtm() {
        String token = Hs256JwtFactory.devToken("customer-3", "atm", "terminal-9");

        AuthenticatedCaller authenticated = authenticator.authenticate(token);

        assertEquals("customer-3", authenticated.callerContext().customerId());
        assertEquals(Channel.ATM, authenticated.callerContext().channel());
        assertEquals("terminal-9", authenticated.callerContext().terminalId().orElseThrow());
        assertEquals(List.of("ROLE_ATM"), authenticated.roles());
    }

    @Test
    @DisplayName("rejects an atm JWT missing its terminal id")
    void rejectsAnAtmJwtMissingItsTerminalId() {
        String token = Hs256JwtFactory.devToken("customer-1", "atm");

        CallerIdentityException exception = assertThrows(
                CallerIdentityException.class, () -> authenticator.authenticate(token));

        assertEquals(CallerIdentityException.Type.INVALID, exception.getType());
    }

    @Test
    @DisplayName("rejects a JWT with an invalid signature")
    void rejectsAJwtWithAnInvalidSignature() {
        String token = Hs256JwtFactory.issue(
                "another-secret-must-be-32-chars-min",
                Hs256JwtFactory.DEFAULT_ISSUER,
                Duration.ofHours(1),
                "customer-1",
                "web",
                null);

        assertThrows(JwtAuthenticationException.class, () -> authenticator.authenticate(token));
    }

    @Test
    @DisplayName("rejects an expired JWT")
    void rejectsAnExpiredJwt() {
        String token = Hs256JwtFactory.issue(
                Hs256JwtFactory.DEFAULT_SECRET,
                Hs256JwtFactory.DEFAULT_ISSUER,
                Duration.ofSeconds(-60),
                "customer-1",
                "web",
                null);

        assertThrows(JwtAuthenticationException.class, () -> authenticator.authenticate(token));
    }

    @Test
    @DisplayName("rejects a JWT with a blank subject")
    void rejectsAJwtWithABlankSubject() {
        String token = Hs256JwtFactory.devToken("  ", "web");

        CallerIdentityException exception = assertThrows(
                CallerIdentityException.class, () -> authenticator.authenticate(token));

        assertEquals(CallerIdentityException.Type.INVALID, exception.getType());
    }

    @Test
    @DisplayName("rejects a JWT without roles")
    void rejectsAJwtWithoutRoles() {
        String token = Hs256JwtFactory.issue(
                Hs256JwtFactory.DEFAULT_SECRET,
                Hs256JwtFactory.DEFAULT_ISSUER,
                Duration.ofHours(1),
                "customer-1",
                "web",
                null,
                List.of());

        assertThrows(JwtAuthenticationException.class, () -> authenticator.authenticate(token));
    }
}
