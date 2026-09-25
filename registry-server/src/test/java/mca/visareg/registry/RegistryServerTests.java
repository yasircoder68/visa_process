package mca.visareg.registry;

import mca.visareg.contracts.registry.FindServiceRequest;
import mca.visareg.contracts.registry.FindServiceResponse;
import mca.visareg.contracts.registry.RegisterServiceRequest;
import mca.visareg.contracts.registry.RegisterServiceResponse;
import mca.visareg.registry.endpoint.RegistryEndpoint;
import mca.visareg.registry.exception.ServiceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RegistryServerTests {

    @Autowired
    private RegistryEndpoint registryEndpoint;

    @Test
    void testRegisterAndFindService() {
        RegisterServiceRequest regReq = new RegisterServiceRequest();
        regReq.setServiceName("VisaApplicationService");
        regReq.setEndpointUrl("http://localhost:8081/ws");

        RegisterServiceResponse regResp = registryEndpoint.registerService(regReq);
        assertNotNull(regResp);
        assertTrue(regResp.isSuccess());

        FindServiceRequest findReq = new FindServiceRequest();
        findReq.setServiceName("VisaApplicationService");

        FindServiceResponse findResp = registryEndpoint.findService(findReq);
        assertNotNull(findResp);
        assertEquals("VisaApplicationService", findResp.getServiceName());
        assertEquals("http://localhost:8081/ws", findResp.getEndpointUrl());
    }

    @Test
    void testFindUnknownServiceThrowsException() {
        FindServiceRequest findReq = new FindServiceRequest();
        findReq.setServiceName("NonExistentService");

        assertThrows(ServiceNotFoundException.class, () -> registryEndpoint.findService(findReq));
    }
}
