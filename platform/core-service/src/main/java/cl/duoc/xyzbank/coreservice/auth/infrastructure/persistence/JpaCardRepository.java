package cl.duoc.xyzbank.coreservice.auth.infrastructure.persistence;

import cl.duoc.xyzbank.coredomain.cards.domain.entities.Card;
import cl.duoc.xyzbank.coredomain.cards.domain.repositories.CardRepository;
import cl.duoc.xyzbank.coredomain.shared.domain.DomainException;
import cl.duoc.xyzbank.coredomain.shared.domain.Id;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCardRepository implements CardRepository {

    private final SpringDataCardRepository jpaRepository;

    public JpaCardRepository(SpringDataCardRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Card card) {
        try {
            jpaRepository.saveAndFlush(toEntity(card));
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw DomainException.conflict("Card was updated concurrently");
        }
    }

    @Override
    public Optional<Card> findByCardNumber(Id cardNumber) {
        // A malformed (non-UUID) card number can never match a stored row; treating it as
        // "not found" rather than letting UUID.fromString's exception escape keeps this
        // lookup from behaving differently for garbage input than for a well-formed but
        // unknown card number, which is exactly the existence oracle callers must not see.
        UUID id;
        try {
            id = UUID.fromString(cardNumber.getValue());
        } catch (IllegalArgumentException malformedCardNumber) {
            return Optional.empty();
        }
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private CardJpaEntity toEntity(Card card) {
        return new CardJpaEntity(
                UUID.fromString(card.getId().getValue()),
                UUID.fromString(card.getCustomerId().getValue()),
                card.getPinHash(),
                card.getConsecutiveFailures(),
                card.isLocked(),
                card.getVersion());
    }

    private Card toDomain(CardJpaEntity entity) {
        return Card.create(
                Id.create(entity.getId().toString()),
                Id.create(entity.getCustomerId().toString()),
                entity.getPinHash(),
                entity.getConsecutiveFailures(),
                entity.isLocked(),
                entity.getVersion());
    }
}
