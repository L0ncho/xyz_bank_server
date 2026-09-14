package cl.duoc.xyzbank.bffweb.dashboard.application.usecases;

import cl.duoc.xyzbank.bffweb.dashboard.application.dto.AccountBalance;
import cl.duoc.xyzbank.bffweb.dashboard.application.dto.AccountSummary;
import cl.duoc.xyzbank.bffweb.dashboard.application.dto.CustomerProfile;
import cl.duoc.xyzbank.bffweb.dashboard.application.dto.DashboardResponse;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.AccountsPort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.CustomerProfilePort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.TransactionsPort;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class DashboardUseCase {

    private static final int LATEST_TRANSACTIONS_PAGE_SIZE = 5;

    private final CustomerProfilePort customerProfilePort;
    private final AccountsPort accountsPort;
    private final TransactionsPort transactionsPort;
    private final Executor executor;

    public DashboardUseCase(
            CustomerProfilePort customerProfilePort,
            AccountsPort accountsPort,
            TransactionsPort transactionsPort,
            Executor executor) {
        this.customerProfilePort = customerProfilePort;
        this.accountsPort = accountsPort;
        this.transactionsPort = transactionsPort;
        this.executor = executor;
    }

    public DashboardResponse execute(String customerId) {
        CustomerProfile profile = customerProfilePort.fetchProfile(customerId);
        List<AccountBalance> accounts = accountsPort.fetchAccountsForCustomer(customerId);

        List<CompletableFuture<AccountSummary>> summaryFutures = accounts.stream()
                .map(account -> supplyAsync(() -> toAccountSummary(account)))
                .toList();
        List<AccountSummary> accountSummaries =
                summaryFutures.stream().map(CompletableFuture::join).toList();

        return new DashboardResponse(profile, accountSummaries);
    }

    /**
     * MDC is thread-local; CorrelationIdClientInterceptor and BearerTokenClientInterceptor read
     * it on whatever thread performs the outbound core-service call. Capturing it here and
     * restoring it on the worker thread is what keeps the correlation id and the caller's
     * bearer token attached to calls dispatched onto the executor instead of the request thread.
     */
    private <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        return CompletableFuture.supplyAsync(() -> {
            if (callerMdc != null) {
                MDC.setContextMap(callerMdc);
            }
            try {
                return supplier.get();
            } finally {
                MDC.clear();
            }
        }, executor);
    }

    private AccountSummary toAccountSummary(AccountBalance account) {
        return new AccountSummary(
                account.id(),
                account.accountNumber(),
                account.balance(),
                account.currency(),
                transactionsPort.fetchLatestTransactions(account.id(), LATEST_TRANSACTIONS_PAGE_SIZE));
    }
}
