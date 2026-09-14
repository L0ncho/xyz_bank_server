package cl.duoc.xyzbank.coreservice.auth.application.usecases;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.cards.domain.repositories.CardRepository;
import cl.duoc.xyzbank.coredomain.cards.domain.services.PinHasher;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import cl.duoc.xyzbank.coreservice.auth.application.dto.PinVerificationOutcome;

public class VerifyPinUseCase {

    private final CardRepository cardRepository;
    private final PinHasher pinHasher;

    public VerifyPinUseCase(CardRepository cardRepository, PinHasher pinHasher) {
        this.cardRepository = cardRepository;
        this.pinHasher = pinHasher;
    }

    public PinVerificationOutcome execute(String cardNumber, String pin) {
        return cardRepository.findByCardNumber(Id.create(cardNumber))
                .map(card -> verify(card, pin))
                .orElse(new PinVerificationOutcome(PinVerificationOutcome.Result.INCORRECT, null));
    }

    private PinVerificationOutcome verify(Card card, String pin) {
        Card.PinVerificationResult cardResult = card.verifyPin(pin, pinHasher);
        cardRepository.save(card);
        PinVerificationOutcome.Result result = toOutcomeResult(cardResult);
        String customerId = result == PinVerificationOutcome.Result.SUCCESS ? card.getCustomerId().getValue() : null;
        return new PinVerificationOutcome(result, customerId);
    }

    private static PinVerificationOutcome.Result toOutcomeResult(Card.PinVerificationResult cardResult) {
        return switch (cardResult) {
            case SUCCESS -> PinVerificationOutcome.Result.SUCCESS;
            case INCORRECT -> PinVerificationOutcome.Result.INCORRECT;
            case LOCKED -> PinVerificationOutcome.Result.LOCKED;
        };
    }
}
