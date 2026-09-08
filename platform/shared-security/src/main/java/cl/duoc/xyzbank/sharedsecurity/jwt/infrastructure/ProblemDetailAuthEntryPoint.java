package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class ProblemDetailAuthEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailHttpWriter writer;

    public ProblemDetailAuthEntryPoint(ObjectMapper objectMapper) {
        this.writer = new ProblemDetailHttpWriter(objectMapper);
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        writer.write(response, HttpStatus.UNAUTHORIZED, "Bearer token is required");
    }
}
