package cl.duoc.xyzbank.coreservice.auth.application.usecases;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.cards.domain.repositories.CardRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.sharedsecurity.callercontext.PinHasher;

public class VerifyPinUseCase {

    private final CardRepository cardRepository;
    private final PinHasher pinHasher;

    public VerifyPinUseCase(CardRepository cardRepository, PinHasher pinHasher) {
        this.cardRepository = cardRepository;
        this.pinHasher = pinHasher;
    }

    public Card.PinVerificationResult execute(String cardNumber, String pin) {
        return cardRepository.findByCardNumber(Id.create(cardNumber))
                .map(card -> verify(card, pin))
                .orElse(Card.PinVerificationResult.INCORRECT);
    }

    private Card.PinVerificationResult verify(Card card, String pin) {
        Card.PinVerificationResult result = card.verifyPin(pin, pinHasher);
        cardRepository.save(card);
        return result;
    }
}
