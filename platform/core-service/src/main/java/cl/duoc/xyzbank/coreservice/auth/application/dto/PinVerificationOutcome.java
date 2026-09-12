package cl.duoc.xyzbank.coreservice.auth.application.dto;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;

public record PinVerificationOutcome(Card.PinVerificationResult result, String customerId) {
}
