package mca.visareg.visa.registration;

import mca.visareg.contracts.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class VisaServerRegistrar {
    private static final Logger log = LoggerFactory.getLogger(VisaServerRegistrar.class);

    private final String registryUrl;
    private final String appPublicUrl;
    private final RegistryClient registryClient;

    public VisaServerRegistrar(
            @Value("${registry.url:http://localhost:8080/ws}") String registryUrl,
            @Value("${app.public-url:http://localhost:8081/ws}") String appPublicUrl) {
        this.registryUrl = registryUrl;
        this.appPublicUrl = appPublicUrl;
        this.registryClient = new RegistryClient();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWithRegistry() {
        Thread thread = new Thread(() -> {
            boolean registered = false;
            while (!registered && !Thread.currentThread().isInterrupted()) {
                log.info("Attempting to register VisaApplicationService ('{}') with Registry at '{}'...", appPublicUrl, registryUrl);
                registered = registryClient.register(registryUrl, "VisaApplicationService", appPublicUrl);
                if (registered) {
                    log.info("Successfully registered VisaApplicationService with Registry!");
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
        thread.setName("visa-registry-registrar");
        thread.setDaemon(true);
        thread.start();
    }
}
