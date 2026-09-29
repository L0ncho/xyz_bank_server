package cl.duoc.xyzbank.coreservice.auth.infrastructure.rest;

@FunctionalInterface
public interface IssuerAccessTokenAuthenticator {

    enum Decision {
        NOT_AN_ISSUER_TOKEN,
        ACCEPTED,
        REJECTED
    }

    Decision authenticate(String bearerToken);
}
