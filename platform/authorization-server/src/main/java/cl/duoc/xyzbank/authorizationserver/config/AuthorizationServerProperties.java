package cl.duoc.xyzbank.authorizationserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "authorization-server")
public record AuthorizationServerProperties(String issuer, User user, List<Client> clients) {

    public record Client(
            String clientId,
            String clientSecret,
            String authenticationMethod,
            List<String> grantTypes,
            List<String> redirectUris,
            List<String> scopes,
            boolean requireProofKey) {
    }

    public record User(String name, String password) {
    }
}
