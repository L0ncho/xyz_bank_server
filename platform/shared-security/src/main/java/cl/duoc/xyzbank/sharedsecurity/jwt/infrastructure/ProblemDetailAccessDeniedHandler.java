package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailHttpWriter writer;

    public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        this.writer = new ProblemDetailHttpWriter(objectMapper);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        writer.write(response, HttpStatus.FORBIDDEN, "Access is denied");
    }
}
