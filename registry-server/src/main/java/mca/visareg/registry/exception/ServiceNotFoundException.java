package mca.visareg.registry.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String serviceName) {
        super("Service not registered: " + serviceName);
    }
}
