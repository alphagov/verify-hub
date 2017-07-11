package uk.gov.ida.saml.core.validators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import java.net.URI;

import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.*;

@RunWith(MockitoJUnitRunner.class)
public class DestinationValidatorTest {

    public static final String EXPECTED_DESTINATION = "http://correct.destination.com";

    private DestinationValidator validator;

    @Before
    public void setup() {
        validator = new DestinationValidator(URI.create(EXPECTED_DESTINATION));
    }

    @Test
    public void validate_shouldThrowExceptionIfDestinationIsAbsent() throws Exception {
        String endpointPath = "/foo/bar";

        validateException(
                SamlTransformationErrorFactory.destinationMissing(URI.create(EXPECTED_DESTINATION + endpointPath)),
                null,
                endpointPath);
    }

    @Test
    public void validate_shouldNotThrowExceptionIfUriMatches() throws Exception {
        validator.validate("http://correct.destination.com/foo/bar", "/foo/bar");
    }

    @Test
    public void validate_shouldBeValidIfPortSpecifiedOnDestinationButNotForSamlProxy() throws Exception {
        validator.validate("http://correct.destination.com:999/foo/bar", "/foo/bar");
    }

    @Test
    public void validate_shouldThrowSamlExceptionIfHostForTheUriOnResponseDoesNotMatchTheSamlReceiverHost() throws Exception {
        final String endPoint = "/foo/bar";
        validateException(
                SamlTransformationErrorFactory.destinationEmpty(URI.create(EXPECTED_DESTINATION + endPoint), "http://saml.com/foo/bar"),
                "http://saml.com/foo/bar", endPoint);
    }

    @Test
    public void validate_shouldThrowSamlExceptionIfHostsMatchButPathsDoNot() throws Exception {

        final String endPoint = "/foo/bar";
        validateException(SamlTransformationErrorFactory.destinationEmpty(URI.create(EXPECTED_DESTINATION + endPoint), "http://saml.com/this/is/a/path"), "http://saml.com/this/is/a/path", endPoint);
    }

    private void validateException(SamlValidationSpecificationFailure failure, final String destination, final String endpointPath) {
        validateFail(() -> validator.validate(destination, endpointPath), failure);
    }
}
