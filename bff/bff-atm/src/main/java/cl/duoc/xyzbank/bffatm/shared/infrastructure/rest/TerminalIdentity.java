package cl.duoc.xyzbank.bffatm.shared.infrastructure.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter to be resolved from the ATM terminal's mTLS client
 * certificate, keeping {@code jakarta.servlet.http.HttpServletRequest} out of controller code
 * (see {@code BffAtmArchitectureTest}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface TerminalIdentity {
}
