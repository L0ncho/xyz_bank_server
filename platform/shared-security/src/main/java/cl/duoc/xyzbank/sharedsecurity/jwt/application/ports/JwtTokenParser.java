package cl.duoc.xyzbank.sharedsecurity.jwt.application.ports;

public interface JwtTokenParser {

    ParsedJwtClaims parse(String compactJwt);
}
