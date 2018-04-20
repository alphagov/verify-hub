package uk.gov.ida.saml.core.test.builders;


import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;

public class SamlEndpointDtoBuilder {

    private SamlEndpointDto.Binding binding = SamlEndpointDto.Binding.POST;
    private URI location = URI.create("https://hub.ida.gov.uk/blah");

    public static SamlEndpointDtoBuilder aSamlEndpointDto(){
        return new SamlEndpointDtoBuilder();
    }

    public SamlEndpointDto build() {
        return new SamlEndpointDto(
                binding,
                location);
    }

    public SamlEndpointDtoBuilder withBinding(SamlEndpointDto.Binding binding) {
        this.binding = binding;
        return this;
    }

    public SamlEndpointDtoBuilder withLocation(URI location) {
        this.location = location;
        return this;
    }
}
