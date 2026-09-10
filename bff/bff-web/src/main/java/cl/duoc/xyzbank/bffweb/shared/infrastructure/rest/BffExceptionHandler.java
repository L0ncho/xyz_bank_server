package cl.duoc.xyzbank.bffweb.shared.infrastructure.rest;

import cl.duoc.xyzbank.bffweb.shared.application.RequestRejectedException;
import cl.duoc.xyzbank.bffweb.shared.infrastructure.adapters.CoreServiceCallException;
import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BffExceptionHandler {

    @ExceptionHandler(CallerIdentityException.class)
    public ProblemDetail handleCallerIdentity(CallerIdentityException exception) {
        HttpStatus status = exception.getType() == CallerIdentityException.Type.INVALID
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.FORBIDDEN;
        return problem(status, exception.getMessage());
    }

    @ExceptionHandler(RequestRejectedException.class)
    public ProblemDetail handleRequestRejected(RequestRejectedException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(CoreServiceCallException.class)
    public ProblemDetail handleCoreServiceCall(CoreServiceCallException exception) {
        return problem(HttpStatusCode.valueOf(exception.getStatus()), exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatusCode status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        HttpStatus resolved = HttpStatus.resolve(status.value());
        if (resolved != null) {
            problem.setTitle(resolved.getReasonPhrase());
        }
        return problem;
    }
}
