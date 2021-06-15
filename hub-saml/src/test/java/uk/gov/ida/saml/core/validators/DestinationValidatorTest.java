package uk.gov.ida.saml.core.validators;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import java.net.URI;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.destinationEmpty;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.destinationMissing;
import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.*;

@ExtendWith(MockitoExtension.class)
public class DestinationValidatorTest {

    private static final String EXPECTED_DESTINATION = "http://correct.destination.com";
    private static final String EXPECTED_ENDPOINT = "/foo/bar";

    private static DestinationValidator validator;

    @BeforeAll
    public static void setup() {
        validator = new DestinationValidator(URI.create(EXPECTED_DESTINATION), EXPECTED_ENDPOINT);
    }

    @Test
    public void validate_shouldThrowExceptionIfDestinationIsAbsent() {
        validateException(
            destinationMissing(URI.create(EXPECTED_DESTINATION + EXPECTED_ENDPOINT)),
            null
        );
    }

    @Test
    public void validate_shouldNotThrowExceptionIfUriMatches() {
        validator.validate("http://correct.destination.com/foo/bar");
    }

    @Test
    public void validate_shouldBeValidIfPortSpecifiedOnDestinationButNotForSamlProxy() {
        validator.validate("http://correct.destination.com:999/foo/bar");
    }

    @Test
    public void validate_shouldThrowSamlExceptionIfHostForTheUriOnResponseDoesNotMatchTheSamlReceiverHost() {
        String invalidDestination = "http://saml.com/foo/bar";
        validateException(
            destinationEmpty(URI.create(EXPECTED_DESTINATION + EXPECTED_ENDPOINT), invalidDestination),
            invalidDestination
        );
    }

    @Test
    public void validate_shouldThrowSamlExceptionIfHostsMatchButPathsDoNot() {
        validateException(
            destinationEmpty(URI.create(EXPECTED_DESTINATION + EXPECTED_ENDPOINT), EXPECTED_DESTINATION + "/this/is/a/path"),
            EXPECTED_DESTINATION + "/this/is/a/path"
        );
    }

    private void validateException(SamlValidationSpecificationFailure failure, final String destination) {
        validateFail(() -> validator.validate(destination), failure);
    }
}
