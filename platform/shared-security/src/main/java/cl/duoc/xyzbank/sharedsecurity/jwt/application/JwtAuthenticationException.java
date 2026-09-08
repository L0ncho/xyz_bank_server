package cl.duoc.xyzbank.sharedsecurity.jwt.application;

public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static JwtAuthenticationException invalid(String message) {
        return new JwtAuthenticationException(message);
    }
}
