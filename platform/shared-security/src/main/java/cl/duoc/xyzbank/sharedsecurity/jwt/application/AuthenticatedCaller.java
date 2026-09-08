package cl.duoc.xyzbank.sharedsecurity.jwt.application;

import cl.duoc.xyzbank.sharedsecurity.callercontext.CallerContext;

import java.util.List;

public record AuthenticatedCaller(CallerContext callerContext, List<String> roles) {
}
