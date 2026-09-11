package cl.duoc.xyzbank.coredomain.auth.unit;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.cards.domain.repositories.CardRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCardRepository implements CardRepository {

    private final Map<String, Card> cards = new ConcurrentHashMap<>();

    @Override
    public void save(Card card) {
        cards.put(card.getId().getValue(), card);
    }

    @Override
    public Optional<Card> findByCardNumber(Id cardNumber) {
        return Optional.ofNullable(cards.get(cardNumber.getValue()));
    }
}
