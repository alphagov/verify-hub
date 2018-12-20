package uk.gov.ida.hub.samlengine.validation.country;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.hub.transformers.inbound.SamlStatusToCountryAuthenticationStatusCodeMapper;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.StatusBuilder.aStatus;
import static uk.gov.ida.saml.core.test.builders.StatusCodeBuilder.aStatusCode;

@RunWith(OpenSAMLMockitoRunner.class)
public class ResponseFromCountryValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Response response;
    @Mock
    private Status status;
    @Mock
    private StatusCode statusCode;
    @Mock
    private Assertion assertion;
    @Mock
    private EncryptedAssertion encryptedAssertion;

    private ResponseFromCountryValidator validator = new ResponseFromCountryValidator(new SamlStatusToCountryAuthenticationStatusCodeMapper());

    @Before
    public void setUp() {
        when(response.getStatus()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn(StatusCode.SUCCESS);
        when(response.getAssertions()).thenReturn(Collections.emptyList());
    }

    @Test
    public void shouldThrowIfResponseIsSuccessfulAndHasUnencryptedAssertions() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Response has unencrypted assertion.");
        when(response.getAssertions()).thenReturn(ImmutableList.of(assertion));

        validator.validateAssertionPresence(response);
    }

    @Test
    public void shouldNotThrowIfResponseIsNotSuccessfulAndHasUnencryptedAssertions() {
        when(statusCode.getValue()).thenReturn(StatusCode.AUTHN_FAILED);

        validator.validateAssertionPresence(response);
    }

    @Test
    public void shouldThrowIfResponseIsSuccessfulButHasNoEncryptedAssertions() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Success response has no unencrypted assertions.");
        when(response.getEncryptedAssertions()).thenReturn(Collections.emptyList());

        validator.validateAssertionPresence(response);
    }

    @Test
    public void shouldThrowIfResponseNotSuccessfulButHasEncryptedAssertions() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Non-success response has unencrypted assertions. Should contain no assertions.");
        when(statusCode.getValue()).thenReturn(StatusCode.AUTHN_FAILED);
        when(response.getEncryptedAssertions()).thenReturn(ImmutableList.of(encryptedAssertion));

        validator.validateAssertionPresence(response);
    }

    @Test
    public void shouldThrowIfResponseIsSuccessfulButHasMultipleEncryptedAssertions() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Response expected to contain 1 assertions. 2 assertion(s) found.");
        when(response.getEncryptedAssertions()).thenReturn(ImmutableList.of(encryptedAssertion, encryptedAssertion));

        validator.validateAssertionPresence(response);
    }

    @Test
    public void validateStatus_shouldNotThrowIfStatusIsResponderWithNoSubStatus() throws Exception {
        Status status = aStatus().withStatusCode(aStatusCode().withValue(StatusCode.RESPONDER).build()).build();
        Response response = aResponse().withStatus(status).withNoDefaultAssertion().build();

        validator.validate(response);
    }
}
