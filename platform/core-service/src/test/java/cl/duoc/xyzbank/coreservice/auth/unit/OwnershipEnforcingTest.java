package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coredomain.accounts.domain.entities.Account;
import cl.duoc.xyzbank.coredomain.accounts.domain.valueobjects.AccountNumber;
import cl.duoc.xyzbank.coredomain.accounts.domain.valueobjects.Money;
import cl.duoc.xyzbank.coredomain.accounts.unit.InMemoryAccountRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coredomain.transactions.domain.entities.Transaction;
import cl.duoc.xyzbank.coredomain.transactions.domain.valueobjects.TransactionType;
import cl.duoc.xyzbank.coredomain.transactions.unit.InMemoryTransactionRepository;
import cl.duoc.xyzbank.coreservice.auth.infrastructure.rest.EnforcementFilter;
import cl.duoc.xyzbank.sharedsecurity.callercontext.Channel;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The EnforcementFilter's ownership check")
class OwnershipEnforcingTest {

    /*
     * Cases:
     * 1. The owning customer's token is let through to a customer endpoint identifying themself
     * 2. The owning customer's token is let through to an account endpoint identifying their own account
     * 3. The owning customer's token is let through to a transaction endpoint identifying a transaction
     *    on their own account
     * 4. A non-owner's token is rejected as not-found for someone else's account
     * 5. A non-owner's token is rejected as not-found for someone else's customer profile
     * 6. A non-owner's token is rejected as not-found for a transaction whose account belongs
     *    to a different customer, resolved through the transaction's account, not any direct
     *    field on the transaction itself
     */

    private static final String SECRET = "unit-test-signing-secret-unit-test-signing-secret";
    private static final Map<String, String> CREDENTIALS = Map.of("web", "web-secret");

    private final JwtCallerContextAdapter tokenAdapter = new JwtCallerContextAdapter(SECRET);
    private final InMemoryAccountRepository accountRepository = new InMemoryAccountRepository();
    private final InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();

    private EnforcementFilter filter() {
        return new EnforcementFilter(true, CREDENTIALS, tokenAdapter, accountRepository, transactionRepository);
    }

    private Account anAccountOwnedBy(Id customerId) {
        Account account = Account.create(
                Id.generate(), AccountNumber.create(randomAccountNumber()), customerId,
                Money.create(new BigDecimal("100.00"), "USD"));
        accountRepository.save(account);
        return account;
    }

    private String randomAccountNumber() {
        return String.valueOf(1000000000L + Math.abs(java.util.UUID.randomUUID().getMostSignificantBits() % 1000000000L));
    }

    @Test
    @DisplayName("lets an owner through to their own customer endpoint")
    void letsAnOwnerThroughToTheirOwnCustomerEndpoint() throws Exception {
        Id customerId = Id.generate();
        String token = tokenAdapter.issue(customerId.getValue(), Channel.WEB, null);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/customers/" + customerId.getValue());
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter().doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("lets an owner through to their own account endpoint")
    void letsAnOwnerThroughToTheirOwnAccountEndpoint() throws Exception {
        Id customerId = Id.generate();
        Account account = anAccountOwnedBy(customerId);
        String token = tokenAdapter.issue(customerId.getValue(), Channel.WEB, null);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/accounts/" + account.getId().getValue() + "/balance");
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter().doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("lets an owner through to a transaction on their own account")
    void letsAnOwnerThroughToATransactionOnTheirOwnAccount() throws Exception {
        Id customerId = Id.generate();
        Account account = anAccountOwnedBy(customerId);
        Transaction transaction = Transaction.create(
                Id.generate(), account.getId(), TransactionType.DEBIT,
                Money.create(new BigDecimal("10.00"), "USD"), LocalDate.now(), null);
        transactionRepository.save(transaction);
        String token = tokenAdapter.issue(customerId.getValue(), Channel.WEB, null);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/transactions/" + transaction.getId().getValue());
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter().doFilter(request, response, chain);

        assertTrue(chainCalled.get());
    }

    @Test
    @DisplayName("rejects a non-owner's request for someone else's account as not-found")
    void rejectsANonOwnersRequestForSomeoneElsesAccountAsNotFound() throws Exception {
        Id ownerId = Id.generate();
        Account account = anAccountOwnedBy(ownerId);
        Id nonOwnerId = Id.generate();
        String token = tokenAdapter.issue(nonOwnerId.getValue(), Channel.WEB, null);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/accounts/" + account.getId().getValue() + "/balance");
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter().doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(404, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }

    @Test
    @DisplayName("rejects a non-owner's request for someone else's customer profile as not-found")
    void rejectsANonOwnersRequestForSomeoneElsesCustomerProfileAsNotFound() throws Exception {
        Id ownerId = Id.generate();
        Id nonOwnerId = Id.generate();
        String token = tokenAdapter.issue(nonOwnerId.getValue(), Channel.WEB, null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/customers/" + ownerId.getValue());
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter().doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(404, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }

    @Test
    @DisplayName("rejects a non-owner's request for a transaction on someone else's account as not-found, "
            + "resolved through the transaction's account")
    void rejectsANonOwnersRequestForATransactionOnSomeoneElsesAccountAsNotFound() throws Exception {
        Id ownerId = Id.generate();
        Account account = anAccountOwnedBy(ownerId);
        Transaction transaction = Transaction.create(
                Id.generate(), account.getId(), TransactionType.DEBIT,
                Money.create(new BigDecimal("10.00"), "USD"), LocalDate.now(), null);
        transactionRepository.save(transaction);
        Id nonOwnerId = Id.generate();
        String token = tokenAdapter.issue(nonOwnerId.getValue(), Channel.WEB, null);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/internal/transactions/" + transaction.getId().getValue());
        request.addHeader("X-Service-Credential", "web-secret");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainCalled.set(true);

        filter().doFilter(request, response, chain);

        assertFalse(chainCalled.get());
        assertEquals(404, response.getStatus());
        assertEquals("application/problem+json", response.getContentType());
    }
}
