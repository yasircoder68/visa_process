package mca.visareg.registration.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public class RegistrationBusinessException extends RuntimeException {
    public RegistrationBusinessException(String message) {
        super(message);
    }

    public RegistrationBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
