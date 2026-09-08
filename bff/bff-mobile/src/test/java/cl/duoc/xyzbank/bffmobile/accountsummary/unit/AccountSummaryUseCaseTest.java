package cl.duoc.xyzbank.bffmobile.accountsummary.unit;

import cl.duoc.xyzbank.bffmobile.accountsummary.application.dto.AccountBalance;
import cl.duoc.xyzbank.bffmobile.accountsummary.application.dto.AccountSummaryResponse;
import cl.duoc.xyzbank.bffmobile.accountsummary.application.ports.AccountsPort;
import cl.duoc.xyzbank.bffmobile.accountsummary.application.usecases.AccountSummaryUseCase;
import cl.duoc.xyzbank.bffmobile.shared.infrastructure.adapters.CoreServiceCallException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("The Account Summary use case")
class AccountSummaryUseCaseTest {

    @Test
    @DisplayName("returns a flat balance payload without transaction history")
    void returnsAFlatBalancePayloadWithoutTransactionHistory() {
        AccountSummaryUseCase useCase = new AccountSummaryUseCase(
                accountId -> new AccountBalance(accountId, new BigDecimal("500.00"), "USD"));

        AccountSummaryResponse response = useCase.execute("account-1");

        assertEquals(new AccountSummaryResponse("account-1", new BigDecimal("500.00"), "USD"), response);
    }

    @Test
    @DisplayName("propagates not-found for an unknown account")
    void propagatesNotFoundForAnUnknownAccount() {
        AccountsPort accountsPort = accountId -> {
            throw new CoreServiceCallException(404, "Account not found");
        };
        AccountSummaryUseCase useCase = new AccountSummaryUseCase(accountsPort);

        CoreServiceCallException exception =
                assertThrows(CoreServiceCallException.class, () -> useCase.execute("unknown"));

        assertEquals(404, exception.getStatus());
    }
}
