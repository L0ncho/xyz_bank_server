package cl.duoc.xyzbank.coredomain.cards.domain.entities;

import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.sharedsecurity.callercontext.PinHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("The Card")
class CardTest {

    /*
     * Cases:
     * 1. Correct PIN succeeds and resets the consecutive-failure count to zero
     * 2. Incorrect PIN fails and increments the consecutive-failure count
     * 3. Exposes the version it was created with, for optimistic locking
     * 4. A third consecutive failure locks the card
     * 5. A locked card rejects even a correct PIN, without changing the failure count
     */

    private final PinHasher hasher = new PinHasher();

    @Test
    @DisplayName("correct pin succeeds and resets the consecutive-failure count to zero")
    void correctPinSucceedsAndResetsFailureCount() {
        Card card = Card.create(Id.generate(), Id.generate(), hasher.hash("1234"), 2, false, 0L);

        Card.PinVerificationResult result = card.verifyPin("1234", hasher);

        assertEquals(Card.PinVerificationResult.SUCCESS, result);
        assertEquals(0, card.getConsecutiveFailures());
    }

    @Test
    @DisplayName("incorrect pin fails and increments the consecutive-failure count")
    void incorrectPinFailsAndIncrementsFailureCount() {
        Card card = Card.create(Id.generate(), Id.generate(), hasher.hash("1234"), 0, false, 0L);

        Card.PinVerificationResult result = card.verifyPin("9999", hasher);

        assertEquals(Card.PinVerificationResult.INCORRECT, result);
        assertEquals(1, card.getConsecutiveFailures());
    }

    @Test
    @DisplayName("exposes the version it was created with")
    void exposesVersion() {
        Card card = Card.create(Id.generate(), Id.generate(), hasher.hash("1234"), 0, false, 7L);

        assertEquals(7L, card.getVersion());
    }

    @Test
    @DisplayName("a third consecutive failure locks the card")
    void thirdConsecutiveFailureLocksCard() {
        Card card = Card.create(Id.generate(), Id.generate(), hasher.hash("1234"), 2, false, 0L);

        Card.PinVerificationResult result = card.verifyPin("9999", hasher);

        assertEquals(Card.PinVerificationResult.LOCKED, result);
        assertTrue(card.isLocked());
        assertEquals(3, card.getConsecutiveFailures());
    }

    @Test
    @DisplayName("a locked card rejects even a correct pin, without changing the failure count")
    void lockedCardRejectsCorrectPin() {
        Card card = Card.create(Id.generate(), Id.generate(), hasher.hash("1234"), 3, true, 0L);

        Card.PinVerificationResult result = card.verifyPin("1234", hasher);

        assertEquals(Card.PinVerificationResult.LOCKED, result);
        assertEquals(3, card.getConsecutiveFailures());
    }
}
