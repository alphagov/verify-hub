package uk.gov.ida.hub.samlengine.validation.country;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EidasAuthnResponseIssuerValidatorTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private EidasAuthnResponseIssuerValidator validator;

    @Mock
    ValidatedResponse validatedResponse;
    @Mock
    Assertion assertion;
    @Mock
    Issuer responseIssuer;
    @Mock
    Issuer assertionIssuer;

    @Before
    public void setup() {
        when(validatedResponse.getIssuer()).thenReturn(responseIssuer);
        when(assertion.getIssuer()).thenReturn(assertionIssuer);
    }

    @Test
    public void validationSucceedsWhenIssuerMatches() {
        when(responseIssuer.getFormat()).thenReturn("issuerFormat");
        when(assertionIssuer.getFormat()).thenReturn("issuerFormat");
        when(responseIssuer.getValue()).thenReturn("issuerValue");
        when(assertionIssuer.getValue()).thenReturn("issuerValue");

        validator.validate(validatedResponse, assertion);
    }

    @Test
    public void validationFailsForDifferentIssuerFormats() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Authn Response issuer format [theResponseIssuerFormat] does not match the assertion issuer format [aDifferentAssertionIssuerFormat]");

        when(responseIssuer.getFormat()).thenReturn("theResponseIssuerFormat");
        when(assertionIssuer.getFormat()).thenReturn("aDifferentAssertionIssuerFormat");

        validator.validate(validatedResponse, assertion);
    }

    @Test
    public void validationFailsForDifferentIssuerValues() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Authn Response issuer [theResponseIssuerValue] does not match the assertion issuer [aDifferentAssertionIssuerValue]");

        when(responseIssuer.getFormat()).thenReturn("issuerFormat");
        when(assertionIssuer.getFormat()).thenReturn("issuerFormat");
        when(responseIssuer.getValue()).thenReturn("theResponseIssuerValue");
        when(assertionIssuer.getValue()).thenReturn("aDifferentAssertionIssuerValue");

        validator.validate(validatedResponse, assertion);
    }
}
