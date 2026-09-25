package mca.visareg.dashboard.config;

import mca.visareg.contracts.client.RegistryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Bean
    public RegistryClient registryClient() {
        return new RegistryClient();
    }
}
