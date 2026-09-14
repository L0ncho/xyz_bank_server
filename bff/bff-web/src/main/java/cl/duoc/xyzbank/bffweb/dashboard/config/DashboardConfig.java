package cl.duoc.xyzbank.bffweb.dashboard.config;

import cl.duoc.xyzbank.bffweb.dashboard.application.ports.AccountsPort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.CustomerProfilePort;
import cl.duoc.xyzbank.bffweb.dashboard.application.ports.TransactionsPort;
import cl.duoc.xyzbank.bffweb.dashboard.application.usecases.DashboardUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class DashboardConfig {

    @Bean
    public Executor dashboardExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public DashboardUseCase dashboardUseCase(
            CustomerProfilePort customerProfilePort,
            AccountsPort accountsPort,
            TransactionsPort transactionsPort,
            Executor dashboardExecutor) {
        return new DashboardUseCase(customerProfilePort, accountsPort, transactionsPort, dashboardExecutor);
    }
}
