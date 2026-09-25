package mca.visareg.dashboard.client;

import mca.visareg.contracts.client.RegistryClient;
import mca.visareg.contracts.registration.*;
import mca.visareg.dashboard.exception.DashboardServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.util.Collections;
import java.util.List;

@Component
public class DashboardRegistrationClient {

    private static final Logger log = LoggerFactory.getLogger(DashboardRegistrationClient.class);
    private static final String SERVICE_NAME = "RegistrationService";

    private final RegistryClient registryClient;
    private final String registryUrl;
    private final WebServiceTemplate webServiceTemplate;

    public DashboardRegistrationClient(RegistryClient registryClient,
                                       @Value("${registry.url:http://localhost:8080/ws}") String registryUrl) {
        this.registryClient = registryClient;
        this.registryUrl = registryUrl;

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("mca.visareg.contracts.registration");
        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Jaxb2Marshaller for DashboardRegistrationClient", e);
        }

        this.webServiceTemplate = new WebServiceTemplate(marshaller);
        this.webServiceTemplate.setMarshaller(marshaller);
        this.webServiceTemplate.setUnmarshaller(marshaller);
    }

    private String resolveEndpoint() {
        try {
            String endpoint = registryClient.find(registryUrl, SERVICE_NAME);
            if (endpoint == null || endpoint.trim().isEmpty()) {
                throw new DashboardServiceException("Service '" + SERVICE_NAME + "' is not registered with Registry at " + registryUrl);
            }
            return endpoint;
        } catch (SoapFaultClientException e) {
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            throw new DashboardServiceException("Cannot connect to Service Registry at " + registryUrl + ": " + e.getMessage());
        }
    }

    public CreateRegistrationResponse createRegistration(String applicationId, String address) {
        String endpoint = resolveEndpoint();
        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId(applicationId);
        request.setAddress(address);

        try {
            return (CreateRegistrationResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault creating registration: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Registration Service unavailable: " + e.getMessage());
        }
    }

    public List<RegistrationInfo> listRegistrations(String officerId) {
        String endpoint = resolveEndpoint();
        ListRegistrationsRequest request = new ListRegistrationsRequest();
        if (officerId != null && !officerId.isBlank()) {
            request.setOfficerId(officerId);
        }

        try {
            ListRegistrationsResponse response = (ListRegistrationsResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
            return response != null ? response.getRegistrations() : Collections.emptyList();
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault listing registrations: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Registration Service unavailable: " + e.getMessage());
        }
    }

    public CompleteRegistrationResponse completeRegistration(String registrationId) {
        String endpoint = resolveEndpoint();
        CompleteRegistrationRequest request = new CompleteRegistrationRequest();
        request.setRegistrationId(registrationId);

        try {
            return (CompleteRegistrationResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault completing registration: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Registration Service unavailable: " + e.getMessage());
        }
    }

    public VerifyDocumentsResponse verifyDocuments(String registrationId) {
        String endpoint = resolveEndpoint();
        VerifyDocumentsRequest request = new VerifyDocumentsRequest();
        request.setRegistrationId(registrationId);

        try {
            return (VerifyDocumentsResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault verifying documents: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Registration Service unavailable: " + e.getMessage());
        }
    }
}
