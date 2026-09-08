package cl.duoc.xyzbank.sharedsecurity.jwt.application;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerContext;
import cl.duoc.xyzbank.sharedsecurity.callercontext.HeaderCallerContextAdapter;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.ports.JwtTokenParser;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.ports.ParsedJwtClaims;

import java.util.List;

public class JwtCallerAuthenticator {

    private final JwtTokenParser jwtTokenParser;

    public JwtCallerAuthenticator(JwtTokenParser jwtTokenParser) {
        this.jwtTokenParser = jwtTokenParser;
    }

    public AuthenticatedCaller authenticate(String compactJwt) {
        ParsedJwtClaims claims = jwtTokenParser.parse(compactJwt);
        List<String> roles = normalizeRoles(claims.roles());
        if (roles.isEmpty()) {
            throw JwtAuthenticationException.invalid("JWT roles are required");
        }
        CallerContext callerContext =
                HeaderCallerContextAdapter.resolve(claims.subject(), claims.channel(), claims.terminalId());
        return new AuthenticatedCaller(callerContext, List.copyOf(roles));
    }

    private static List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .toList();
    }
}
