package cl.duoc.xyzbank.coreservice.auth.config;

import cl.duoc.xyzbank.coreservice.auth.infrastructure.rest.EnforcementFilter;
import cl.duoc.xyzbank.sharedsecurity.callercontext.JwtCallerContextAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class EnforcementFilterConfig {

    @Value("${security.enforcement.enabled}")
    private boolean enforcementEnabled;

    @Value("${security.service-credentials.web}")
    private String webServiceCredential;

    @Value("${security.service-credentials.mobile}")
    private String mobileServiceCredential;

    @Value("${security.service-credentials.atm}")
    private String atmServiceCredential;

    @Bean
    public FilterRegistrationBean<EnforcementFilter> enforcementFilter(JwtCallerContextAdapter tokenAdapter) {
        Map<String, String> serviceCredentials = Map.of(
                "web", webServiceCredential,
                "mobile", mobileServiceCredential,
                "atm", atmServiceCredential);
        FilterRegistrationBean<EnforcementFilter> registration = new FilterRegistrationBean<>(
                new EnforcementFilter(enforcementEnabled, serviceCredentials, tokenAdapter));
        registration.addUrlPatterns("/internal/*");
        registration.setOrder(2);
        return registration;
    }
}
