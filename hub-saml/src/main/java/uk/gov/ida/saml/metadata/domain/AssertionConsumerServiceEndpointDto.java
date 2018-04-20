package uk.gov.ida.saml.metadata.domain;


import java.net.URI;

public class AssertionConsumerServiceEndpointDto extends SamlEndpointDto {

    private boolean isDefault;
    private int index;

    @SuppressWarnings("unused") // needed for JAXB
    private AssertionConsumerServiceEndpointDto() {
    }

    public AssertionConsumerServiceEndpointDto(URI location, boolean isDefault, int index) {
        super(SamlEndpointDto.Binding.POST, location); // Assertion Consumer Services must always be post
        this.isDefault = isDefault;
        this.index = index;
    }

    public boolean getIsDefault() {
        return isDefault;
    }

    public int getIndex() {
        return index;
    }

    public static AssertionConsumerServiceEndpointDto createAssertionConsumerService(URI location, boolean isDefault, int index) {
        return new AssertionConsumerServiceEndpointDto(location, isDefault, index);
    }
}
