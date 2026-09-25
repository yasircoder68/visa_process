package mca.visareg.registration;

import mca.visareg.contracts.registration.*;
import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.registration.client.VisaClient;
import mca.visareg.registration.endpoint.RegistrationEndpoint;
import mca.visareg.registration.exception.RegistrationBusinessException;
import mca.visareg.registration.geocoding.GeocodingResult;
import mca.visareg.registration.geocoding.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RegistrationServerTests {

    @Autowired
    private RegistrationEndpoint registrationEndpoint;

    @MockBean
    private VisaClient visaClient;

    @MockBean
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        when(geocodingService.resolve(anyString()))
                .thenReturn(new GeocodingResult(13.0418, 80.2341, "Chennai"));
    }

    private ApplicationInfo createApprovedVisa(String visaId) throws Exception {
        ApplicationInfo approvedVisa = new ApplicationInfo();
        approvedVisa.setId(visaId);
        approvedVisa.setApplicantName("John Doe");
        approvedVisa.setStatus("APPROVED");
        LocalDate validDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(180);
        approvedVisa.setValidUntil(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                validDate.getYear(), validDate.getMonthValue(), validDate.getDayOfMonth(), 0
        ));
        return approvedVisa;
    }

    @Test
    void testCreateRegistrationWithApprovedVisa() throws Exception {
        when(visaClient.getApplication("VISA001")).thenReturn(createApprovedVisa("VISA001"));

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId("VISA001");
        request.setAddress("T Nagar, Chennai");

        CreateRegistrationResponse response = registrationEndpoint.createRegistration(request);
        assertNotNull(response);
        assertNotNull(response.getRegistration());
        assertEquals("APPOINTMENT_SCHEDULED", response.getRegistration().getStatus());
        assertEquals("VISA001", response.getRegistration().getApplicationId());
        assertEquals("Chennai", response.getRegistration().getDistrict());
        assertNotNull(response.getRegistration().getAssignedOfficer());
        assertNotNull(response.getRegistration().getAppointmentDate());
    }

    @Test
    void testCreateRegistrationWithUnapprovedVisaThrowsFault() {
        ApplicationInfo submittedVisa = new ApplicationInfo();
        submittedVisa.setId("VISA002");
        submittedVisa.setStatus("SUBMITTED");

        when(visaClient.getApplication("VISA002")).thenReturn(submittedVisa);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId("VISA002");
        request.setAddress("T Nagar, Chennai");

        RegistrationBusinessException ex = assertThrows(RegistrationBusinessException.class,
                () -> registrationEndpoint.createRegistration(request));
        assertTrue(ex.getMessage().contains("not APPROVED"));
    }

    @Test
    void testCreateRegistrationNoOfficerAvailableThrowsFault() throws Exception {
        when(visaClient.getApplication("VISA003")).thenReturn(createApprovedVisa("VISA003"));
        when(geocodingService.resolve("Unknown Place, Salem"))
                .thenReturn(new GeocodingResult(11.6643, 78.1460, "Salem"));

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId("VISA003");
        request.setAddress("Unknown Place, Salem");

        RegistrationBusinessException ex = assertThrows(RegistrationBusinessException.class,
                () -> registrationEndpoint.createRegistration(request));
        assertTrue(ex.getMessage().contains("No FRRO officer available in Salem"));
    }

    @Test
    void testCompleteRegistrationWorkflow() throws Exception {
        when(visaClient.getApplication("VISA001")).thenReturn(createApprovedVisa("VISA001"));

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId("VISA001");
        request.setAddress("T Nagar, Chennai");

        CreateRegistrationResponse createResp = registrationEndpoint.createRegistration(request);
        String regId = createResp.getRegistration().getId();
        assertEquals("APPOINTMENT_SCHEDULED", createResp.getRegistration().getStatus());

        CompleteRegistrationRequest completeReq = new CompleteRegistrationRequest();
        completeReq.setRegistrationId(regId);

        CompleteRegistrationResponse completeResp = registrationEndpoint.completeRegistration(completeReq);
        assertNotNull(completeResp);
        assertEquals("REGISTERED", completeResp.getRegistration().getStatus());
    }

    @Test
    void testVerifyDocuments() throws Exception {
        when(visaClient.getApplication("VISA001")).thenReturn(createApprovedVisa("VISA001"));

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId("VISA001");
        request.setAddress("T Nagar, Chennai");

        CreateRegistrationResponse createResp = registrationEndpoint.createRegistration(request);
        String regId = createResp.getRegistration().getId();

        VerifyDocumentsRequest verifyReq = new VerifyDocumentsRequest();
        verifyReq.setRegistrationId(regId);

        VerifyDocumentsResponse verifyResp = registrationEndpoint.verifyDocuments(verifyReq);
        assertNotNull(verifyResp);
        assertEquals("DOCS_VERIFIED", verifyResp.getRegistration().getStatus());
    }
}
