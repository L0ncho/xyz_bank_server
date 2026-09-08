package cl.duoc.xyzbank.bffmobile.accountsummary.application.dto;

import java.math.BigDecimal;

public record AccountSummaryResponse(String accountId, BigDecimal balance, String currency) {
}
