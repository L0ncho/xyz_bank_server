package cl.duoc.xyzbank.bffmobile.accountsummary.application.usecases;

import cl.duoc.xyzbank.bffmobile.accountsummary.application.dto.AccountBalance;
import cl.duoc.xyzbank.bffmobile.accountsummary.application.dto.AccountSummaryResponse;
import cl.duoc.xyzbank.bffmobile.accountsummary.application.ports.AccountsPort;

public class AccountSummaryUseCase {

    private final AccountsPort accountsPort;

    public AccountSummaryUseCase(AccountsPort accountsPort) {
        this.accountsPort = accountsPort;
    }

    public AccountSummaryResponse execute(String accountId) {
        AccountBalance balance = accountsPort.fetchBalance(accountId);
        return new AccountSummaryResponse(balance.accountId(), balance.balance(), balance.currency());
    }
}
