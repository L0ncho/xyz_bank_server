package cl.duoc.xyzbank.bffweb.dashboard.config;

import cl.duoc.xyzbank.bffweb.dashboard.application.ports.AccountsPort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.CustomerProfilePort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.InterestPort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.TransactionsPort;
import cl.duoc.xyzbank.bffweb.dashboard.application.usecases.DashboardUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class DashboardConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public DashboardUseCase dashboardUseCase(
            CustomerProfilePort customerProfilePort,
            AccountsPort accountsPort,
            TransactionsPort transactionsPort,
            InterestPort interestPort,
            Clock clock) {
        return new DashboardUseCase(
                customerProfilePort, accountsPort, transactionsPort, interestPort, clock);
    }
}
