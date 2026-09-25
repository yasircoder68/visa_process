package mca.visareg.registration.client;

import mca.visareg.contracts.client.RegistryClient;
import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.contracts.visa.GetApplicationRequest;
import mca.visareg.contracts.visa.GetApplicationResponse;
import mca.visareg.registration.exception.RegistrationBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

@Component
public class VisaClient {
    private static final Logger log = LoggerFactory.getLogger(VisaClient.class);

    private final String registryUrl;
    private final RegistryClient registryClient;
    private final WebServiceTemplate webServiceTemplate;

    public VisaClient(@Value("${registry.url:http://localhost:8080/ws}") String registryUrl) {
        this.registryUrl = registryUrl;
        this.registryClient = new RegistryClient();

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("mca.visareg.contracts.visa");
        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Jaxb2Marshaller for VisaClient", e);
        }

        this.webServiceTemplate = new WebServiceTemplate(marshaller);
        this.webServiceTemplate.setMarshaller(marshaller);
        this.webServiceTemplate.setUnmarshaller(marshaller);
    }

    public ApplicationInfo getApplication(String applicationId) {
        String endpointUrl;
        try {
            endpointUrl = registryClient.find(registryUrl, "VisaApplicationService");
        } catch (Exception e) {
            log.error("Failed to query registry at '{}': {}", registryUrl, e.getMessage());
            throw new RegistrationBusinessException("Unable to contact Service Registry: " + e.getMessage());
        }

        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            throw new RegistrationBusinessException("VisaApplicationService is not registered with Registry Server");
        }

        GetApplicationRequest request = new GetApplicationRequest();
        request.setApplicationId(applicationId);

        try {
            GetApplicationResponse response = (GetApplicationResponse) webServiceTemplate.marshalSendAndReceive(endpointUrl, request);
            if (response == null || response.getApplication() == null) {
                throw new RegistrationBusinessException("Application not found with ID: " + applicationId);
            }
            return response.getApplication();
        } catch (SoapFaultClientException e) {
            String faultString = e.getSoapFault() != null ? e.getSoapFault().getFaultStringOrReason() : e.getMessage();
            log.warn("Remote SOAP fault from VisaApplicationService: {}", faultString);
            throw new RegistrationBusinessException("Visa service error: " + faultString);
        } catch (Exception e) {
            log.error("Error connecting to VisaApplicationService at '{}': {}", endpointUrl, e.getMessage());
            throw new RegistrationBusinessException("Failed to communicate with VisaApplicationService: " + e.getMessage());
        }
    }
}
