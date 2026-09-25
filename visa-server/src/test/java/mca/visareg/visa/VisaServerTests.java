package mca.visareg.visa;

import mca.visareg.contracts.visa.*;
import mca.visareg.visa.endpoint.VisaEndpoint;
import mca.visareg.visa.exception.VisaBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VisaServerTests {

    @Autowired
    private VisaEndpoint visaEndpoint;

    @Test
    void testFullVisaLifecycle() {
        // 1. Submit Application
        SubmitApplicationRequest submitReq = new SubmitApplicationRequest();
        submitReq.setApplicantName("John Doe");
        submitReq.setPassportNumber("P12345678");
        submitReq.setVisaType("STUDENT");
        submitReq.setPortOfArrival("Chennai");

        SubmitApplicationResponse submitResp = visaEndpoint.submitApplication(submitReq);
        assertNotNull(submitResp);
        assertNotNull(submitResp.getApplicationId());
        assertTrue(submitResp.getApplicationId().startsWith("VISA"));
        assertEquals("SUBMITTED", submitResp.getStatus());

        String appId = submitResp.getApplicationId();

        // 2. Get Application
        GetApplicationRequest getReq = new GetApplicationRequest();
        getReq.setApplicationId(appId);
        GetApplicationResponse getResp = visaEndpoint.getApplication(getReq);
        assertNotNull(getResp.getApplication());
        assertEquals("John Doe", getResp.getApplication().getApplicantName());
        assertEquals("SUBMITTED", getResp.getApplication().getStatus());

        // 3. Approve Application
        UpdateApplicationStatusRequest updateReq = new UpdateApplicationStatusRequest();
        updateReq.setApplicationId(appId);
        updateReq.setStatus("APPROVED");
        UpdateApplicationStatusResponse updateResp = visaEndpoint.updateApplicationStatus(updateReq);
        assertNotNull(updateResp.getApplication());
        assertEquals("APPROVED", updateResp.getApplication().getStatus());

        // 4. Issue ETA
        IssueETARequest etaReq = new IssueETARequest();
        etaReq.setApplicationId(appId);
        etaReq.setValidDays(180);
        IssueETAResponse etaResp = visaEndpoint.issueETA(etaReq);
        assertNotNull(etaResp.getApplication());
        assertEquals("ETA-" + appId, etaResp.getApplication().getEtaNumber());
        assertNotNull(etaResp.getApplication().getValidUntil());

        // 5. List Applications
        ListApplicationsRequest listReq = new ListApplicationsRequest();
        listReq.setStatus("APPROVED");
        ListApplicationsResponse listResp = visaEndpoint.listApplications(listReq);
        assertNotNull(listResp);
        assertTrue(listResp.getApplications().stream().anyMatch(a -> a.getId().equals(appId)));
    }

    @Test
    void testInvalidStatusTransitionThrowsException() {
        SubmitApplicationRequest submitReq = new SubmitApplicationRequest();
        submitReq.setApplicantName("Jane Doe");
        submitReq.setPassportNumber("P87654321");
        submitReq.setVisaType("TOURIST");
        submitReq.setPortOfArrival("Delhi");

        SubmitApplicationResponse submitResp = visaEndpoint.submitApplication(submitReq);
        String appId = submitResp.getApplicationId();

        // Try to issue ETA before approval -> should throw
        IssueETARequest etaReq = new IssueETARequest();
        etaReq.setApplicationId(appId);
        assertThrows(VisaBusinessException.class, () -> visaEndpoint.issueETA(etaReq));
    }
}
