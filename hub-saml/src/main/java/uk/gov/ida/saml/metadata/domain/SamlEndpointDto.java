package uk.gov.ida.saml.metadata.domain;

import java.net.URI;

public class SamlEndpointDto {

    private Binding binding;
    private URI location;

    SamlEndpointDto() {}

    public SamlEndpointDto(Binding binding, URI location) {
        this.binding = binding;
        this.location = location;
    }

    public enum Binding {
        POST,
        SOAP
    }

    public static SamlEndpointDto createPostBinding(URI location){
        return new SamlEndpointDto(Binding.POST, location);
    }

    public static SamlEndpointDto createSoapBinding(URI location) {
        return new SamlEndpointDto(Binding.SOAP, location);
    }

    public Binding getBinding() {
        return binding;
    }

    public URI getLocation() {
        return location;
    }
}
