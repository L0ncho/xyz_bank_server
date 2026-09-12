package cl.duoc.xyzbank.bffatm.shared.config;

import cl.duoc.xyzbank.bffatm.shared.infrastructure.rest.CallerContextInterceptor;
import cl.duoc.xyzbank.bffatm.shared.infrastructure.rest.TerminalIdentityArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CallerContextInterceptor callerContextInterceptor;
    private final TerminalIdentityArgumentResolver terminalIdentityArgumentResolver;

    public WebMvcConfig(
            CallerContextInterceptor callerContextInterceptor,
            TerminalIdentityArgumentResolver terminalIdentityArgumentResolver) {
        this.callerContextInterceptor = callerContextInterceptor;
        this.terminalIdentityArgumentResolver = terminalIdentityArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(callerContextInterceptor)
                .excludePathPatterns("/actuator/**", "/v3/api-docs", "/v3/api-docs/**", "/pin-verifications");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(terminalIdentityArgumentResolver);
    }
}
