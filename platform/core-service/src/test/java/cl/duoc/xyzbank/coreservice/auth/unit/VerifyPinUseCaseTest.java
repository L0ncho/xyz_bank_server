package cl.duoc.xyzbank.coreservice.auth.unit;

import cl.duoc.xyzbank.coredomain.auth.unit.InMemoryCardRepository;
import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.usecases.VerifyPinUseCase;
import cl.duoc.xyzbank.sharedsecurity.callercontext.PinHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The VerifyPin use case")
class VerifyPinUseCaseTest {

    /*
     * Cases:
     * 1. A correct PIN succeeds and resets the card's failure count
     * 2. An incorrect PIN fails and increments the card's failure count
     * 3. An unknown card number fails identically to an incorrect PIN (no existence oracle)
     * 4. A third consecutive failure locks the card and is reported distinctly from a plain incorrect PIN
     * 5. A locked card rejects a subsequent correct PIN
     */

    private final PinHasher hasher = new PinHasher();
    private final InMemoryCardRepository cardRepository = new InMemoryCardRepository();
    private final VerifyPinUseCase useCase = new VerifyPinUseCase(cardRepository, hasher);

    @Test
    @DisplayName("a correct pin succeeds and resets the card's failure count")
    void correctPinSucceedsAndResetsFailureCount() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), hasher.hash("1234"), 2, false, 0L));

        Card.PinVerificationResult result = useCase.execute(cardNumber.getValue(), "1234");

        assertEquals(Card.PinVerificationResult.SUCCESS, result);
        assertEquals(0, cardRepository.findByCardNumber(cardNumber).orElseThrow().getConsecutiveFailures());
    }

    @Test
    @DisplayName("an incorrect pin fails and increments the card's failure count")
    void incorrectPinFailsAndIncrementsFailureCount() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), hasher.hash("1234"), 0, false, 0L));

        Card.PinVerificationResult result = useCase.execute(cardNumber.getValue(), "9999");

        assertEquals(Card.PinVerificationResult.INCORRECT, result);
        assertEquals(1, cardRepository.findByCardNumber(cardNumber).orElseThrow().getConsecutiveFailures());
    }

    @Test
    @DisplayName("an unknown card number fails identically to an incorrect pin")
    void unknownCardNumberFailsIdenticallyToIncorrectPin() {
        Card.PinVerificationResult result = useCase.execute(Id.generate().getValue(), "1234");

        assertEquals(Card.PinVerificationResult.INCORRECT, result);
    }

    @Test
    @DisplayName("a third consecutive failure locks the card")
    void thirdConsecutiveFailureLocksCard() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), hasher.hash("1234"), 2, false, 0L));

        Card.PinVerificationResult result = useCase.execute(cardNumber.getValue(), "9999");

        assertEquals(Card.PinVerificationResult.LOCKED, result);
        assertTrue(cardRepository.findByCardNumber(cardNumber).orElseThrow().isLocked());
    }

    @Test
    @DisplayName("a locked card rejects a subsequent correct pin")
    void lockedCardRejectsSubsequentCorrectPin() {
        Id cardNumber = Id.generate();
        cardRepository.save(Card.create(cardNumber, Id.generate(), hasher.hash("1234"), 3, true, 0L));

        Card.PinVerificationResult result = useCase.execute(cardNumber.getValue(), "1234");

        assertEquals(Card.PinVerificationResult.LOCKED, result);
    }
}
