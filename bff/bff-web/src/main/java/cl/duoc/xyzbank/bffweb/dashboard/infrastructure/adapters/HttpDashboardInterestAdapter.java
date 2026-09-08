package cl.duoc.xyzbank.bffweb.dashboard.infrastructure.adapters;

import cl.duoc.xyzbank.bffweb.dashboard.application.dto.InterestSummary;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.InterestPort;
import cl.duoc.xyzbank.bffweb.shared.infrastructure.adapters.CoreServiceCalls;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class HttpDashboardInterestAdapter implements InterestPort {

    private final RestClient coreServiceClient;

    public HttpDashboardInterestAdapter(RestClient coreServiceClient) {
        this.coreServiceClient = coreServiceClient;
    }

    @Override
    public InterestSummary fetchSummary(String accountId, String year) {
        InterestSummaryWire wire = CoreServiceCalls.fetch(() -> coreServiceClient.get()
                .uri("/internal/accounts/{accountId}/interest-summary?year={year}", accountId, year)
                .retrieve()
                .body(InterestSummaryWire.class));
        return new InterestSummary(
                wire.accountId(),
                wire.year(),
                wire.openingBalance(),
                wire.closingBalance(),
                wire.interestRate(),
                wire.interestAmount(),
                wire.currency());
    }

    private record InterestSummaryWire(
            String accountId,
            int year,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal interestRate,
            BigDecimal interestAmount,
            String currency) {
    }
}
