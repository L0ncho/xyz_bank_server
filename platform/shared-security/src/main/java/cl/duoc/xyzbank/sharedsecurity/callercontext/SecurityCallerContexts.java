package cl.duoc.xyzbank.sharedsecurity.callercontext;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityCallerContexts {

    private SecurityCallerContexts() {
    }

    public static CallerContext requireAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CallerContext callerContext) {
            return callerContext;
        }
        throw CallerIdentityException.invalid("Caller identity is required");
    }
}
