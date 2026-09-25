package mca.visareg.visa.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class VisaBusinessException extends RuntimeException {
    public VisaBusinessException(String message) {
        super(message);
    }
}
