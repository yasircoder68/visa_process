package mca.visareg.dashboard.client;

import mca.visareg.contracts.client.RegistryClient;
import mca.visareg.contracts.visa.*;
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
public class DashboardVisaClient {

    private static final Logger log = LoggerFactory.getLogger(DashboardVisaClient.class);
    private static final String SERVICE_NAME = "VisaApplicationService";

    private final RegistryClient registryClient;
    private final String registryUrl;
    private final WebServiceTemplate webServiceTemplate;

    public DashboardVisaClient(RegistryClient registryClient,
                               @Value("${registry.url:http://localhost:8080/ws}") String registryUrl) {
        this.registryClient = registryClient;
        this.registryUrl = registryUrl;

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("mca.visareg.contracts.visa");
        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Jaxb2Marshaller for DashboardVisaClient", e);
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

    public SubmitApplicationResponse submitApplication(String applicantName, String passportNumber, String visaType, String portOfArrival) {
        String endpoint = resolveEndpoint();
        SubmitApplicationRequest request = new SubmitApplicationRequest();
        request.setApplicantName(applicantName);
        request.setPassportNumber(passportNumber);
        request.setVisaType(visaType);
        request.setPortOfArrival(portOfArrival);

        try {
            return (SubmitApplicationResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault submitting visa application: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Visa Service unavailable: " + e.getMessage());
        }
    }

    public ApplicationInfo getApplication(String applicationId) {
        String endpoint = resolveEndpoint();
        GetApplicationRequest request = new GetApplicationRequest();
        request.setApplicationId(applicationId);

        try {
            GetApplicationResponse response = (GetApplicationResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
            if (response != null && response.getApplication() != null) {
                log.info("DEBUG getApplication: app={}, id={}, name={}, status={}",
                        response.getApplication(),
                        response.getApplication().getId(),
                        response.getApplication().getApplicantName(),
                        response.getApplication().getStatus());
            } else {
                log.info("DEBUG getApplication: response={}, app={}", response, response != null ? response.getApplication() : null);
            }
            return response != null ? response.getApplication() : null;
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault getting visa application '{}': {}", applicationId, e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Visa Service unavailable: " + e.getMessage());
        }
    }

    public List<ApplicationInfo> listApplications(String status) {
        String endpoint = resolveEndpoint();
        ListApplicationsRequest request = new ListApplicationsRequest();
        if (status != null && !status.isBlank()) {
            request.setStatus(status);
        }

        try {
            ListApplicationsResponse response = (ListApplicationsResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
            return response != null ? response.getApplications() : Collections.emptyList();
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault listing applications: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Visa Service unavailable: " + e.getMessage());
        }
    }

    public UpdateApplicationStatusResponse updateApplicationStatus(String id, String status) {
        String endpoint = resolveEndpoint();
        UpdateApplicationStatusRequest request = new UpdateApplicationStatusRequest();
        request.setApplicationId(id);
        request.setStatus(status);

        try {
            return (UpdateApplicationStatusResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault updating application status: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Visa Service unavailable: " + e.getMessage());
        }
    }

    public IssueETAResponse issueETA(String id) {
        String endpoint = resolveEndpoint();
        IssueETARequest request = new IssueETARequest();
        request.setApplicationId(id);

        try {
            return (IssueETAResponse) webServiceTemplate.marshalSendAndReceive(endpoint, request);
        } catch (SoapFaultClientException e) {
            log.warn("SOAP Fault issuing ETA: {}", e.getFaultStringOrReason());
            throw new DashboardServiceException(e.getFaultStringOrReason());
        } catch (Exception e) {
            log.error("Error communicating with {}: {}", SERVICE_NAME, e.getMessage());
            throw new DashboardServiceException("Visa Service unavailable: " + e.getMessage());
        }
    }
}
