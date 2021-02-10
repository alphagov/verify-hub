package uk.gov.ida.saml.hub.validators.response.matchingservice;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import uk.gov.ida.saml.security.AssertionDecrypter;
import uk.gov.ida.saml.security.SamlAssertionsSignatureValidator;
import uk.gov.ida.saml.security.exception.SamlFailedToDecryptException;
import uk.gov.ida.saml.security.validators.ValidatedResponse;
import uk.gov.ida.saml.security.validators.signature.SamlResponseSignatureValidator;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceResponseValidatorTest {
    @Mock
    private SamlResponseSignatureValidator samlResponseSignatureValidator;
    @Mock
    private AssertionDecrypter assertionDecrypter;
    @Mock
    private AssertionDecrypter badAssertionDecrypter;
    @Mock
    private SamlAssertionsSignatureValidator samlAssertionsSignatureValidator;
    @Mock
    private EncryptedResponseFromMatchingServiceValidator encryptedResponseFromMatchingServiceValidator;
    @Mock
    private ResponseAssertionsFromMatchingServiceValidator responseAssertionsFromMatchingServiceValidator;
    @Mock
    private Response response;

    @Rule
    public final ExpectedException samlValidationException = ExpectedException.none();

    private MatchingServiceResponseValidator validator;

    @Before
    public void setUp() {
        validator = new MatchingServiceResponseValidator(
            encryptedResponseFromMatchingServiceValidator,
            samlResponseSignatureValidator,
            Arrays.asList(assertionDecrypter, badAssertionDecrypter),
            samlAssertionsSignatureValidator,
            responseAssertionsFromMatchingServiceValidator);
    }

    @Test
    public void shouldValidateResponseIsEncrypted() {
        validator.validate(response);
        verify(encryptedResponseFromMatchingServiceValidator).validate(response);
    }

    @Test
    public void shouldValidateSamlResponseSignature() {
        validator.validate(response);
        verify(samlResponseSignatureValidator).validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void shouldValidateSamlAssertionSignature() {
        Assertion assertion = mock(Assertion.class);
        List<Assertion> assertions = ImmutableList.of(assertion);
        ValidatedResponse validatedResponse = mock(ValidatedResponse.class);

        when(samlResponseSignatureValidator.validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(validatedResponse);
        when(assertionDecrypter.decryptAssertions(validatedResponse)).thenReturn(assertions);
        when(badAssertionDecrypter.decryptAssertions(validatedResponse)).thenThrow(SamlFailedToDecryptException.class);

        validator.validate(response);

        verify(samlAssertionsSignatureValidator).validate(assertions, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME);
    }

    @Test
    public void shouldThrowIfAllDecryptersFail() {
        ValidatedResponse validatedResponse = mock(ValidatedResponse.class);

        when(samlResponseSignatureValidator.validate(response, AttributeAuthorityDescriptor.DEFAULT_ELEMENT_NAME)).thenReturn(validatedResponse);
        when(assertionDecrypter.decryptAssertions(validatedResponse)).thenThrow(SamlFailedToDecryptException.class);
        when(badAssertionDecrypter.decryptAssertions(validatedResponse)).thenThrow(SamlFailedToDecryptException.class);

        samlValidationException.expect(SamlFailedToDecryptException.class);
        samlValidationException.expectMessage("Could not decrypt MSA assertions with any of the decrypters");
        validator.validate(response);
    }
}