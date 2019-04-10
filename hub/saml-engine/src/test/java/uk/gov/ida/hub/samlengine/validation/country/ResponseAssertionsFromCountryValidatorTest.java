package uk.gov.ida.hub.samlengine.validation.country;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnStatement;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.assertion.IdentityProviderAssertionValidator;
import uk.gov.ida.saml.core.validators.assertion.AuthnStatementAssertionValidator;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ResponseAssertionsFromCountryValidatorTest {
    private static final String EXPECTED_RECIPIENT_ID = "expected-recipient-id";

    @Mock
    private IdentityProviderAssertionValidator assertionValidator;
    @Mock
    private EidasAttributeStatementAssertionValidator eidasAttributeStatementAssertionValidator;
    @Mock
    private AuthnStatementAssertionValidator authnStatementAssertionValidator;
    @Mock
    private EidasAuthnResponseIssuerValidator authnResponseIssuerValidator;
    @Mock
    private ValidatedResponse validatedResponse;
    @Mock
    private Assertion assertion;
    @Mock
    private AuthnStatement authnStatement;

    private ResponseAssertionsFromCountryValidator validator;

    @Before
    public void setup() {
        validator = new ResponseAssertionsFromCountryValidator(assertionValidator,
            eidasAttributeStatementAssertionValidator,
            authnStatementAssertionValidator,
            authnResponseIssuerValidator,
            EXPECTED_RECIPIENT_ID);

        when(validatedResponse.isSuccess()).thenReturn(true);
        when(assertion.getAuthnStatements()).thenReturn(ImmutableList.of(authnStatement));
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfMultipleAuthnStatements() {
        when(assertion.getAuthnStatements()).thenReturn(ImmutableList.of(authnStatement, authnStatement));

        validator.validate(validatedResponse, assertion);
    }

    @Test
    public void shouldValidateAssertion() {
        validator.validate(validatedResponse, assertion);

        verify(assertionValidator).validate(assertion, validatedResponse.getInResponseTo(), EXPECTED_RECIPIENT_ID);
        verify(authnStatementAssertionValidator).validate(assertion);
        verify(eidasAttributeStatementAssertionValidator).validate(assertion);
    }

    @Test
    public void shouldNotValidateAssertionContentsIfResponseIsFailure() {
        when(validatedResponse.isSuccess()).thenReturn(false);

        validator.validate(validatedResponse, assertion);

        verifyZeroInteractions(authnStatementAssertionValidator, eidasAttributeStatementAssertionValidator);
    }

    @Test
    public void validateIssuer() {
        validator.validate(validatedResponse, assertion);

        verify(authnResponseIssuerValidator).validate(validatedResponse, assertion);
        verifyNoMoreInteractions(authnResponseIssuerValidator);
    }
}
