package uk.gov.ida.saml.hub.transformers.inbound.providers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.eidas.logging.EidasAttributesLogger;
import uk.gov.ida.saml.hub.transformers.inbound.IdaResponseFromIdpUnmarshaller;
import uk.gov.ida.saml.hub.validators.response.idp.IdpResponseValidator;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DecoratedSamlResponseToIdaResponseIssuedByIdpTransformerTest {

    private DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer transformer;

    @Mock
    private IdaResponseFromIdpUnmarshaller idaResponseUnmarshaller;
    @Mock
    private IdpResponseValidator idpResponseValidator;
    @Mock
    private EidasAttributesLogger eidasAttributesLogger;
    @Mock
    private Response response;
    @Mock
    private ValidatedAssertions validatedAssertions;
    @Mock
    private ValidatedResponse validatedResponse;
    @Mock
    private Assertion assertion;

    @Before
    public void setUp() throws Exception {
        transformer = new DecoratedSamlResponseToIdaResponseIssuedByIdpTransformer(idpResponseValidator, idaResponseUnmarshaller);
    }

    @Test
    public void testAttributesNeverLoggedWhenEidasAttributesLoggerAbsent() {
        when(idpResponseValidator.getValidatedAssertions()).thenReturn(validatedAssertions);
        when(idpResponseValidator.getValidatedResponse()).thenReturn(validatedResponse);
        transformer.apply(response);
        verify(eidasAttributesLogger, never()).logEidasAttributesAsHash(assertion, response);
        verify(validatedAssertions, never()).getMatchingDatasetAssertion();
        verify(idaResponseUnmarshaller).fromSaml(validatedResponse, validatedAssertions);
    }

    @Test
    public void testAttributesLoggedWhenEidasAttributesLoggerPresent() {
        when(idpResponseValidator.getValidatedAssertions()).thenReturn(validatedAssertions);
        when(validatedAssertions.getMatchingDatasetAssertion()).thenReturn(Optional.of(assertion));
        when(idpResponseValidator.getValidatedResponse()).thenReturn(validatedResponse);
        transformer.setEidasAttributesLogger(eidasAttributesLogger);
        transformer.apply(response);
        verify(idaResponseUnmarshaller).fromSaml(validatedResponse, validatedAssertions);
        verify(validatedAssertions).getMatchingDatasetAssertion();
        verify(eidasAttributesLogger).logEidasAttributesAsHash(assertion, response);
        verify(idaResponseUnmarshaller).fromSaml(validatedResponse, validatedAssertions);
    }

    @Test
    public void testAttributesNotLoggedWhenMatchingDatasetAssertionsEmpty() {
        when(idpResponseValidator.getValidatedAssertions()).thenReturn(validatedAssertions);
        when(validatedAssertions.getMatchingDatasetAssertion()).thenReturn(Optional.empty());
        when(idpResponseValidator.getValidatedResponse()).thenReturn(validatedResponse);
        transformer.setEidasAttributesLogger(eidasAttributesLogger);
        transformer.apply(response);
        verify(validatedAssertions).getMatchingDatasetAssertion();
        verify(eidasAttributesLogger, never()).logEidasAttributesAsHash(assertion, response);
        verify(idpResponseValidator).validate(response);
        verify(idaResponseUnmarshaller).fromSaml(validatedResponse, validatedAssertions);
        verifyNoMoreInteractions(eidasAttributesLogger);
    }

}