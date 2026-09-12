package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.unit.InMemoryCardRepository;
import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.cards.domain.services.PinHasher;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.dto.PinVerificationOutcome;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.VerifyPinUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The VerifyPin use case")
class VerifyPinUseCaseTest {

    /*
     * Cases:
     * 1. A correct PIN succeeds, resets the card's failure count, and reports the owning customer id
     * 2. An incorrect PIN fails and increments the card's failure count
     * 3. An unknown card number fails identically to an incorrect PIN (no existence oracle)
     * 4. A third consecutive failure locks the card and is reported distinctly from a plain incorrect PIN
     * 5. A locked card rejects a subsequent correct PIN
     */

    private final cl.duoc.xyzbank.sharedsecurity.callercontext.PinHasher bcryptHasher =
            new cl.duoc.xyzbank.sharedsecurity.callercontext.PinHasher();
    private final PinHasher hasher = bcryptHasher::matches;
    private final InMemoryCardRepository cardRepository = new InMemoryCardRepository();
    private final VerifyPinUseCase useCase = new VerifyPinUseCase(cardRepository, hasher);

    @Test
    @DisplayName("a correct pin succeeds, resets the card's failure count, and reports the owning customer id")
    void correctPinSucceedsAndResetsFailureCount() {
        Id cardNumber = Id.generate();
        Id customerId = Id.generate();
        cardRepository.save(Card.create(cardNumber, customerId, bcryptHasher.hash("1234"), 2, false, 0L));

        PinVerificationOutcome outcome = useCase.execute(cardNumber.getValue(), "1234");

        assertEquals(PinVerificationOutcome.Result.SUCCESS, outcome.result());
        assertEquals(customerId.getValue(), outcome.customerId());
        assertEquals(0, cardRepository.findByCardNumber(cardNumber).orElseThrow().getConsecutiveFailures());
    }

    @Test
    @DisplayName("an incorrect pin fails and increments the card's failure count")
    void incorrectPinFailsAndIncrementsFailureCount() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), bcryptHasher.hash("1234"), 0, false, 0L));

        PinVerificationOutcome outcome = useCase.execute(cardNumber.getValue(), "9999");

        assertEquals(PinVerificationOutcome.Result.INCORRECT, outcome.result());
        assertNull(outcome.customerId());
        assertEquals(1, cardRepository.findByCardNumber(cardNumber).orElseThrow().getConsecutiveFailures());
    }

    @Test
    @DisplayName("an unknown card number fails identically to an incorrect pin")
    void unknownCardNumberFailsIdenticallyToIncorrectPin() {
        PinVerificationOutcome outcome = useCase.execute(Id.generate().getValue(), "1234");

        assertEquals(PinVerificationOutcome.Result.INCORRECT, outcome.result());
        assertNull(outcome.customerId());
    }

    @Test
    @DisplayName("a third consecutive failure locks the card")
    void thirdConsecutiveFailureLocksCard() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), bcryptHasher.hash("1234"), 2, false, 0L));

        PinVerificationOutcome outcome = useCase.execute(cardNumber.getValue(), "9999");

        assertEquals(PinVerificationOutcome.Result.LOCKED, outcome.result());
        assertNull(outcome.customerId());
        assertTrue(cardRepository.findByCardNumber(cardNumber).orElseThrow().isLocked());
    }

    @Test
    @DisplayName("a locked card rejects a subsequent correct pin")
    void lockedCardRejectsSubsequentCorrectPin() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), bcryptHasher.hash("1234"), 3, true, 0L));

        PinVerificationOutcome outcome = useCase.execute(cardNumber.getValue(), "1234");

        assertEquals(PinVerificationOutcome.Result.LOCKED, outcome.result());
        assertNull(outcome.customerId());
    }
}
