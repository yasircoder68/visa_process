package mca.visareg.registration.endpoint;

import mca.visareg.contracts.registration.*;
import mca.visareg.registration.entity.Registration;
import mca.visareg.registration.service.RegistrationService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.List;

@Endpoint
public class RegistrationEndpoint {

    public static final String NAMESPACE_URI = "http://visareg.mca/registration";

    private final RegistrationService registrationService;

    public RegistrationEndpoint(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createRegistrationRequest")
    @ResponsePayload
    public CreateRegistrationResponse createRegistration(@RequestPayload CreateRegistrationRequest request) {
        Registration reg = registrationService.createRegistration(request.getApplicationId(), request.getAddress());

        CreateRegistrationResponse response = new CreateRegistrationResponse();
        response.setRegistration(registrationService.toDto(reg));
        response.setMessage("Registration created and verified successfully with ID: " + reg.getId());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "verifyDocumentsRequest")
    @ResponsePayload
    public VerifyDocumentsResponse verifyDocuments(@RequestPayload VerifyDocumentsRequest request) {
        Registration reg = registrationService.verifyDocuments(request.getRegistrationId());

        VerifyDocumentsResponse response = new VerifyDocumentsResponse();
        response.setRegistration(registrationService.toDto(reg));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "scheduleAppointmentRequest")
    @ResponsePayload
    public ScheduleAppointmentResponse scheduleAppointment(@RequestPayload ScheduleAppointmentRequest request) {
        Registration reg = registrationService.scheduleAppointment(request.getRegistrationId());

        ScheduleAppointmentResponse response = new ScheduleAppointmentResponse();
        response.setRegistration(registrationService.toDto(reg));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "completeRegistrationRequest")
    @ResponsePayload
    public CompleteRegistrationResponse completeRegistration(@RequestPayload CompleteRegistrationRequest request) {
        Registration reg = registrationService.completeRegistration(request.getRegistrationId());

        CompleteRegistrationResponse response = new CompleteRegistrationResponse();
        response.setRegistration(registrationService.toDto(reg));
        response.setMessage("Registration completed for ID: " + reg.getId());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "listRegistrationsRequest")
    @ResponsePayload
    public ListRegistrationsResponse listRegistrations(@RequestPayload ListRegistrationsRequest request) {
        String officerId = (request != null) ? request.getOfficerId() : null;
        List<Registration> list = registrationService.listRegistrations(officerId);

        ListRegistrationsResponse response = new ListRegistrationsResponse();
        for (Registration reg : list) {
            response.getRegistrations().add(registrationService.toDto(reg));
        }
        return response;
    }
}
