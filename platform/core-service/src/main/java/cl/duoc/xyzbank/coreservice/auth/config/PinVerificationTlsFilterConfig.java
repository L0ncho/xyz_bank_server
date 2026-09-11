package cl.duoc.xyzbank.coreservice.auth.config;

import cl.duoc.xyzbank.coreservice.auth.infrastructure.rest.PinVerificationTlsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PinVerificationTlsFilterConfig {

    @Bean
    public FilterRegistrationBean<PinVerificationTlsFilter> pinVerificationTlsFilter() {
        FilterRegistrationBean<PinVerificationTlsFilter> registration =
                new FilterRegistrationBean<>(new PinVerificationTlsFilter());
        registration.addUrlPatterns("/internal/auth/atm/pin-verifications");
        return registration;
    }
}
