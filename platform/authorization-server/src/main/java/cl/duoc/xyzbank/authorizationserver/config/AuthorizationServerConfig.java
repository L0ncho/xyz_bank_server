package cl.duoc.xyzbank.authorizationserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient.Builder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(AuthorizationServerProperties.class)
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        MediaTypeRequestMatcher html = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        html.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"), html));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(AuthorizationServerProperties properties) {
        AuthorizationServerProperties.User user = properties.user();
        return new InMemoryUserDetailsManager(User.withUsername(user.name())
                .password("{noop}" + user.password())
                .roles("CUSTOMER")
                .build());
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(AuthorizationServerProperties properties) {
        return AuthorizationServerSettings.builder()
                .issuer(properties.issuer())
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(AuthorizationServerProperties properties) {
        RegisteredClient[] clients = properties.clients().stream()
                .map(this::toRegisteredClient)
                .toArray(RegisteredClient[]::new);
        return new InMemoryRegisteredClientRepository(clients);
    }

    private RegisteredClient toRegisteredClient(AuthorizationServerProperties.Client client) {
        Builder builder = RegisteredClient.withId(client.clientId())
                .clientId(client.clientId())
                .clientAuthenticationMethod(new ClientAuthenticationMethod(client.authenticationMethod()))
                .tokenSettings(TokenSettings.builder().accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED).build());
        if (client.clientSecret() != null) {
            builder.clientSecret("{noop}" + client.clientSecret());
        }
        for (String grantType : client.grantTypes()) {
            builder.authorizationGrantType(new AuthorizationGrantType(grantType));
        }
        for (String redirectUri : client.redirectUris()) {
            builder.redirectUri(redirectUri);
        }
        for (String scope : client.scopes()) {
            builder.scope(scope);
        }
        builder.clientSettings(ClientSettings.builder()
                .requireProofKey(client.requireProofKey())
                .build());
        return builder.build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = signingKeyPair();
        RSAKey signingKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(signingKey));
    }

    private static KeyPair signingKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate the signing key", exception);
        }
    }
}
