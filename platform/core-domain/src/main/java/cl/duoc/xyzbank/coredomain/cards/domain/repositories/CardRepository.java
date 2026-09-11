package cl.duoc.xyzbank.coredomain.cards.domain.repositories;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

import java.util.Optional;

public interface CardRepository {
    void save(Card card);

    Optional<Card> findByCardNumber(Id cardNumber);
}
