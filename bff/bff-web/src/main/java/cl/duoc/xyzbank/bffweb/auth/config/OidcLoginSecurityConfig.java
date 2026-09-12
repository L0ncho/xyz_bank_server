package cl.duoc.xyzbank.bffweb.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Wires spring-boot-starter-oauth2-client for the OIDC authorization-code + PKCE handshake
 * only (design.md Decision 3). Every request is otherwise permitted through unauthenticated
 * and stateless: the rest of bff-web keeps resolving identity from its own short-lived JWT
 * cookie via CallerContextInterceptor, never from Spring Security's SecurityContext/session.
 */
@Configuration
public class OidcLoginSecurityConfig {

    private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            AuthenticationSuccessHandler oidcLoginSuccessHandler,
            AuthenticationFailureHandler oidcLoginFailureHandler)
            throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2Login(oauth2Login -> oauth2Login
                        .authorizationEndpoint(authorization -> authorization.authorizationRequestResolver(
                                pkceAuthorizationRequestResolver(clientRegistrationRepository)))
                        .successHandler(oidcLoginSuccessHandler)
                        .failureHandler(oidcLoginFailureHandler));
        return http.build();
    }

    private OAuth2AuthorizationRequestResolver pkceAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }
}
