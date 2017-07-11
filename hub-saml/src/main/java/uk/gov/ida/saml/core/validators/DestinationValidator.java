package uk.gov.ida.saml.core.validators;

import com.google.common.base.Throwables;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import java.net.URI;
import java.net.URISyntaxException;

public class DestinationValidator {

    private final URI expectedDestinationHost;
    private static final int NO_PORT = -1;

    public DestinationValidator(URI expectedDestinationHost) {
        this.expectedDestinationHost = expectedDestinationHost;
    }

    /*
    Validate that the destination sent to us matches the configured host & the given path

    Path is added because we have to do validation on both Responses & Requests
     */
    public void validate(String destination, String endpointPath) {
        URI expectedUri;
        try {
            expectedUri = new URI(expectedDestinationHost.getScheme(), expectedDestinationHost.getHost(), endpointPath, null);
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }

        if(destination == null) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.destinationMissing(expectedUri);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }

        if (!expectedUri.equals(getDestinationUriWithoutPort(destination))) {
            SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.destinationEmpty(expectedUri, destination);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
    }

    private URI getDestinationUriWithoutPort(String destination) {
        URI destinationURI = URI.create(destination);

        if (destinationURI.getPort() == NO_PORT) {
            return destinationURI;
        }
        try {
            return new URI(destinationURI.getScheme(), destinationURI.getHost(), destinationURI.getPath(), null);
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

}
