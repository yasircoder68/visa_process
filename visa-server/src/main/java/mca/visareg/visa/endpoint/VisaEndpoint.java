package mca.visareg.visa.endpoint;

import mca.visareg.contracts.visa.*;
import mca.visareg.visa.entity.Application;
import mca.visareg.visa.service.VisaService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.List;

@Endpoint
public class VisaEndpoint {

    public static final String NAMESPACE_URI = "http://visareg.mca/visa";

    private final VisaService visaService;

    public VisaEndpoint(VisaService visaService) {
        this.visaService = visaService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "submitApplicationRequest")
    @ResponsePayload
    public SubmitApplicationResponse submitApplication(@RequestPayload SubmitApplicationRequest request) {
        Application app = visaService.submitApplication(
                request.getApplicantName(),
                request.getPassportNumber(),
                request.getVisaType(),
                request.getPortOfArrival()
        );

        SubmitApplicationResponse response = new SubmitApplicationResponse();
        response.setApplicationId(app.getId());
        response.setStatus(app.getStatus());
        response.setMessage("Application submitted successfully with ID: " + app.getId());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getApplicationRequest")
    @ResponsePayload
    public GetApplicationResponse getApplication(@RequestPayload GetApplicationRequest request) {
        Application app = visaService.getApplication(request.getApplicationId());

        GetApplicationResponse response = new GetApplicationResponse();
        response.setApplication(visaService.toDto(app));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "updateApplicationStatusRequest")
    @ResponsePayload
    public UpdateApplicationStatusResponse updateApplicationStatus(@RequestPayload UpdateApplicationStatusRequest request) {
        Application app = visaService.updateApplicationStatus(request.getApplicationId(), request.getStatus());

        UpdateApplicationStatusResponse response = new UpdateApplicationStatusResponse();
        response.setApplication(visaService.toDto(app));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "issueETARequest")
    @ResponsePayload
    public IssueETAResponse issueETA(@RequestPayload IssueETARequest request) {
        Application app = visaService.issueETA(request.getApplicationId(), request.getValidDays());

        IssueETAResponse response = new IssueETAResponse();
        response.setApplication(visaService.toDto(app));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "listApplicationsRequest")
    @ResponsePayload
    public ListApplicationsResponse listApplications(@RequestPayload ListApplicationsRequest request) {
        String status = (request != null) ? request.getStatus() : null;
        List<Application> apps = visaService.listApplications(status);

        ListApplicationsResponse response = new ListApplicationsResponse();
        for (Application app : apps) {
            response.getApplications().add(visaService.toDto(app));
        }
        return response;
    }
}
