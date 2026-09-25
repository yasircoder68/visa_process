package mca.visareg.registry.service;

import mca.visareg.registry.exception.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServiceRegistryService {
    private static final Logger log = LoggerFactory.getLogger(ServiceRegistryService.class);

    private final Map<String, String> registryMap = new ConcurrentHashMap<>();

    public void register(String serviceName, String endpointUrl) {
        registryMap.put(serviceName, endpointUrl);
        log.info("Registered service: '{}' -> '{}'", serviceName, endpointUrl);
    }

    public String find(String serviceName) {
        String url = registryMap.get(serviceName);
        if (url == null || url.trim().isEmpty()) {
            log.warn("Service lookup failed: '{}'", serviceName);
            throw new ServiceNotFoundException(serviceName);
        }
        log.info("Found service: '{}' -> '{}'", serviceName, url);
        return url;
    }
}
