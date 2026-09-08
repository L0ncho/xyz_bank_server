package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class CallerContextAuthentication extends AbstractAuthenticationToken {

    private final CallerContext callerContext;

    private CallerContextAuthentication(CallerContext callerContext, List<String> roles) {
        super(roles.stream().map(SimpleGrantedAuthority::new).toList());
        this.callerContext = callerContext;
        setAuthenticated(true);
    }

    public static CallerContextAuthentication authenticated(CallerContext callerContext, List<String> roles) {
        return new CallerContextAuthentication(callerContext, List.copyOf(roles));
    }

    public CallerContext callerContext() {
        return callerContext;
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return callerContext;
    }
}
