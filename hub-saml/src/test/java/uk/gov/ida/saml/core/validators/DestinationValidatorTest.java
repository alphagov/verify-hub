package uk.gov.ida.saml.core.validators;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.destinationEmpty;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.destinationMissing;
import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.*;

@RunWith(MockitoJUnitRunner.class)
public class DestinationValidatorTest {

    private static final String EXPECTED_DESTINATION = "http://correct.destination.com";
    private static final String EXPECTED_ENDPOINT = "/foo/bar";

    private DestinationValidator validator;

    @Before
    public void setup() throws URISyntaxException {
        validator = new DestinationValidator(URI.create(EXPECTED_DESTINATION), EXPECTED_ENDPOINT);
    }

    @Test
    public void validate_shouldThrowExceptionIfDestinationIsAbsent() throws Exception {
        validateException(
            destinationMissing(URI.create(EXPECTED_DESTINATION + EXPECTED_ENDPOINT)),
            null
        );
    }

    @Test
    public void validate_shouldNotThrowExceptionIfUriMatches() throws Exception {
        validator.validate("http://correct.destination.com/foo/bar");
    }

    @Test
    public void validate_shouldBeValidIfPortSpecifiedOnDestinationButNotForSamlProxy() throws Exception {
        validator.validate("http://correct.destination.com:999/foo/bar");
    }

    @Test
    public void validate_shouldThrowSamlExceptionIfHostForTheUriOnResponseDoesNotMatchTheSamlReceiverHost() throws Exception {
        String invalidDestination = "http://saml.com/foo/bar";
        validateException(
            destinationEmpty(URI.create(EXPECTED_DESTINATION + EXPECTED_ENDPOINT), invalidDestination),
            invalidDestination
        );
    }

    @Test
    public void validate_shouldThrowSamlExceptionIfHostsMatchButPathsDoNot() throws Exception {
        validateException(
            destinationEmpty(URI.create(EXPECTED_DESTINATION + EXPECTED_ENDPOINT), EXPECTED_DESTINATION + "/this/is/a/path"),
            EXPECTED_DESTINATION + "/this/is/a/path"
        );
    }

    private void validateException(SamlValidationSpecificationFailure failure, final String destination) {
        validateFail(() -> validator.validate(destination), failure);
    }
}
