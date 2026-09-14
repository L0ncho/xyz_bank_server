package cl.duoc.xyzbank.bffweb.shared.infrastructure.concurrency;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Decorates an {@link Executor} so every submitted task runs with the submitting thread's MDC
 * context instead of an empty one. CorrelationIdClientInterceptor and BearerTokenClientInterceptor
 * read MDC on whatever thread performs an outbound core-service call, so a task dispatched onto a
 * worker thread must have the caller's correlation id and bearer token restored before it runs.
 */
public class MdcPropagatingExecutor implements Executor {

    private final Executor delegate;

    public MdcPropagatingExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        delegate.execute(() -> {
            if (callerMdc != null) {
                MDC.setContextMap(callerMdc);
            }
            try {
                command.run();
            } finally {
                MDC.clear();
            }
        });
    }
}
