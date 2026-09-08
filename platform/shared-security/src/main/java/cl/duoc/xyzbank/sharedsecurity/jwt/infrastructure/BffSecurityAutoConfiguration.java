package cl.duoc.xyzbank.sharedsecurity.jwt.infrastructure;

import cl.duoc.xyzbank.sharedsecurity.jwt.application.JwtCallerAuthenticator;
import cl.duoc.xyzbank.sharedsecurity.jwt.application.ports.JwtTokenParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(XyzBankSecurityProperties.class)
public class BffSecurityAutoConfiguration {

    @Bean
    JwtTokenParser jwtTokenParser(XyzBankSecurityProperties properties) {
        return new NimbusJwtTokenParser(properties.getJwt().getSecret(), properties.getJwt().getIssuer());
    }

    @Bean
    JwtCallerAuthenticator jwtCallerAuthenticator(JwtTokenParser jwtTokenParser) {
        return new JwtCallerAuthenticator(jwtTokenParser);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtCallerAuthenticator jwtCallerAuthenticator,
            ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtCallerAuthenticator, objectMapper);
    }

    @Bean
    ProblemDetailAuthEntryPoint problemDetailAuthEntryPoint(ObjectMapper objectMapper) {
        return new ProblemDetailAuthEntryPoint(objectMapper);
    }

    @Bean
    ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
        return new ProblemDetailAccessDeniedHandler(objectMapper);
    }

    @Bean
    SecurityFilterChain bffSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ProblemDetailAuthEntryPoint problemDetailAuthEntryPoint,
            ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemDetailAuthEntryPoint)
                        .accessDeniedHandler(problemDetailAccessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
