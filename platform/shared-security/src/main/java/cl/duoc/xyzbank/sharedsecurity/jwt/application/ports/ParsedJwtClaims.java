package cl.duoc.xyzbank.sharedsecurity.jwt.application.ports;

import java.util.List;

public record ParsedJwtClaims(String subject, String channel, String terminalId, List<String> roles) {
}
