package mca.visareg.registration;

import mca.visareg.contracts.registration.CreateRegistrationRequest;
import mca.visareg.contracts.registration.CreateRegistrationResponse;
import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.registration.client.VisaClient;
import mca.visareg.registration.endpoint.RegistrationEndpoint;
import mca.visareg.registration.exception.RegistrationBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RegistrationServerTests {

    @Autowired
    private RegistrationEndpoint registrationEndpoint;

    @MockBean
    private VisaClient visaClient;

    @Test
    void testCreateRegistrationWithApprovedVisa() throws Exception {
        ApplicationInfo approvedVisa = new ApplicationInfo();
        approvedVisa.setId("VISA001");
        approvedVisa.setApplicantName("John Doe");
        approvedVisa.setStatus("APPROVED");
        LocalDate validDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(180);
        approvedVisa.setValidUntil(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                validDate.getYear(), validDate.getMonthValue(), validDate.getDayOfMonth(), 0
        ));

        when(visaClient.getApplication("VISA001")).thenReturn(approvedVisa);

        CreateRegistrationRequest request = new CreateRegistrationRequest();
        request.setApplicationId("VISA001");
        request.setAddress("T Nagar, Chennai");

        CreateRegistrationResponse response = registrationEndpoint.createRegistration(request);
        assertNotNull(response);
        assertNotNull(response.getRegistration());
        assertEquals("DOCS_VERIFIED", response.getRegistration().getStatus());
        assertEquals("VISA001", response.getRegistration().getApplicationId());
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

        assertThrows(RegistrationBusinessException.class, () -> registrationEndpoint.createRegistration(request));
    }
}
