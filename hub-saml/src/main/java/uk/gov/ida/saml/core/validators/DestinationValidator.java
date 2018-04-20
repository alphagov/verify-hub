package uk.gov.ida.saml.core.validators;

import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import uk.gov.ida.saml.hub.exception.SamlValidationException;

import java.net.URI;
import java.net.URISyntaxException;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.destinationEmpty;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.destinationMissing;

public class DestinationValidator {

    private final URI expectedUri;

    public DestinationValidator(URI expectedDestinationHost, String expectedEndpoint) {
        expectedUri = uriWithoutPort(expectedDestinationHost, expectedEndpoint);
    }

    /*
    Validate that the destination sent to us matches the configured host & the given path

    Path is added because we have to do validation on both Responses & Requests
     */
    public void validate(String destination) {
        if(destination == null) throw new SamlValidationException(destinationMissing(expectedUri));

        URI destinationURI = URI.create(destination);

        URI destinationURIWithoutPort;
        destinationURIWithoutPort = uriWithoutPort(destinationURI, destinationURI.getPath());

        if (!expectedUri.equals(destinationURIWithoutPort))
            throw new SamlValidationException(destinationEmpty(expectedUri, destination));
    }

    private URI uriWithoutPort(URI destinationURI, String endpoint) {
        try {
            return new URI(destinationURI.getScheme(), destinationURI.getHost(), endpoint, null);
        } catch (URISyntaxException e) {
            throw new SamlValidationException(SamlTransformationErrorFactory.destinationInvalid(destinationURI, endpoint));
        }
    }

}
