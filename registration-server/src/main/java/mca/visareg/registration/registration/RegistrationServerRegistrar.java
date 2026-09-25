package mca.visareg.registration.registration;

import mca.visareg.contracts.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationServerRegistrar {
    private static final Logger log = LoggerFactory.getLogger(RegistrationServerRegistrar.class);

    private final String registryUrl;
    private final String appPublicUrl;
    private final RegistryClient registryClient;

    public RegistrationServerRegistrar(
            @Value("${registry.url:http://localhost:8080/ws}") String registryUrl,
            @Value("${app.public-url:http://localhost:8082/ws}") String appPublicUrl) {
        this.registryUrl = registryUrl;
        this.appPublicUrl = appPublicUrl;
        this.registryClient = new RegistryClient();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWithRegistry() {
        Thread thread = new Thread(() -> {
            boolean registered = false;
            while (!registered && !Thread.currentThread().isInterrupted()) {
                log.info("Attempting to register RegistrationService ('{}') with Registry at '{}'...", appPublicUrl, registryUrl);
                registered = registryClient.register(registryUrl, "RegistrationService", appPublicUrl);
                if (registered) {
                    log.info("Successfully registered RegistrationService with Registry!");
                } else {
                    log.warn("Registration failed. Retrying in 5 seconds...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        thread.setName("registration-registry-registrar");
        thread.setDaemon(true);
        thread.start();
    }
}
