package cl.duoc.xyzbank.bffweb.dashboard.application.dto;

import java.math.BigDecimal;

public record InterestSummary(
        String accountId,
        int year,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal interestRate,
        BigDecimal interestAmount,
        String currency) {
}
