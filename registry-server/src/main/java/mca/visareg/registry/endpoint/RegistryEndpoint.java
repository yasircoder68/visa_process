package mca.visareg.registry.endpoint;

import mca.visareg.contracts.registry.FindServiceRequest;
import mca.visareg.contracts.registry.FindServiceResponse;
import mca.visareg.contracts.registry.RegisterServiceRequest;
import mca.visareg.contracts.registry.RegisterServiceResponse;
import mca.visareg.registry.service.ServiceRegistryService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class RegistryEndpoint {

    public static final String NAMESPACE_URI = "http://visareg.mca/registry";

    private final ServiceRegistryService registryService;

    public RegistryEndpoint(ServiceRegistryService registryService) {
        this.registryService = registryService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "registerServiceRequest")
    @ResponsePayload
    public RegisterServiceResponse registerService(@RequestPayload RegisterServiceRequest request) {
        registryService.register(request.getServiceName(), request.getEndpointUrl());

        RegisterServiceResponse response = new RegisterServiceResponse();
        response.setSuccess(true);
        response.setMessage("Service registered successfully: " + request.getServiceName());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "findServiceRequest")
    @ResponsePayload
    public FindServiceResponse findService(@RequestPayload FindServiceRequest request) {
        String endpointUrl = registryService.find(request.getServiceName());

        FindServiceResponse response = new FindServiceResponse();
        response.setServiceName(request.getServiceName());
        response.setEndpointUrl(endpointUrl);
        return response;
    }
}
