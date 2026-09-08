package cl.duoc.xyzbank.bffatm.shared.infrastructure.rest;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerContext;
import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerIdentityException;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.SecurityCallerContexts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CallerContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        CallerContext callerContext = SecurityCallerContexts.requireAuthenticated();
        if (callerContext.channel() != Channel.ATM) {
            throw CallerIdentityException.forbidden("This endpoint requires the atm channel");
        }
        String terminalHeader = request.getHeader("X-Terminal-Id");
        String claimedTerminalId = callerContext.terminalId().orElse(null);
        if (terminalHeader == null || !terminalHeader.equals(claimedTerminalId)) {
            throw CallerIdentityException.forbidden("Terminal id does not match the authenticated terminal");
        }
        return true;
    }
}
