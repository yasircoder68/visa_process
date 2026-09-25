package mca.visareg.contracts.client;

import mca.visareg.contracts.registry.FindServiceRequest;
import mca.visareg.contracts.registry.FindServiceResponse;
import mca.visareg.contracts.registry.RegisterServiceRequest;
import mca.visareg.contracts.registry.RegisterServiceResponse;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistryClient {
    private static final Logger log = Logger.getLogger(RegistryClient.class.getName());

    private final WebServiceTemplate webServiceTemplate;

    public RegistryClient() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("mca.visareg.contracts.registry");
        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Jaxb2Marshaller for RegistryClient", e);
        }
        this.webServiceTemplate = new WebServiceTemplate(marshaller);
        this.webServiceTemplate.setMarshaller(marshaller);
        this.webServiceTemplate.setUnmarshaller(marshaller);
    }

    public boolean register(String registryUrl, String serviceName, String endpointUrl) {
        RegisterServiceRequest request = new RegisterServiceRequest();
        request.setServiceName(serviceName);
        request.setEndpointUrl(endpointUrl);
        try {
            RegisterServiceResponse response = (RegisterServiceResponse) webServiceTemplate.marshalSendAndReceive(registryUrl, request);
            return response != null && response.isSuccess();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to register service '" + serviceName + "' at '" + registryUrl + "': " + e.getMessage());
            return false;
        }
    }

    public String find(String registryUrl, String serviceName) {
        FindServiceRequest request = new FindServiceRequest();
        request.setServiceName(serviceName);
        FindServiceResponse response = (FindServiceResponse) webServiceTemplate.marshalSendAndReceive(registryUrl, request);
        if (response != null) {
            return response.getEndpointUrl();
        }
        return null;
    }
}
