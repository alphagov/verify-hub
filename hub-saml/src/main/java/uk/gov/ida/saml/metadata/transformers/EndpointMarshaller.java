package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.Endpoint;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;

public class EndpointMarshaller {

    public SamlEndpointDto toDto(Endpoint endpoint) {
        URI location = URI.create(endpoint.getLocation());
        switch (endpoint.getBinding()) {
            case SAMLConstants.SAML2_POST_BINDING_URI:
                return SamlEndpointDto.createPostBinding(location);
            case SAMLConstants.SAML2_SOAP11_BINDING_URI:
                return SamlEndpointDto.createSoapBinding(location);
            default:
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unrecognisedBinding(endpoint.getBinding());
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }
}
