package uk.gov.ida.saml.hub.transformers.inbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.builders.SignatureBuilder;
import uk.gov.ida.saml.hub.domain.InboundResponseFromIdp;
import uk.gov.ida.saml.security.validators.ValidatedAssertions;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.PassthroughAssertionBuilder.aPassthroughAssertion;
import static uk.gov.ida.saml.core.test.builders.AuthnStatementBuilder.anAuthnStatement;

@RunWith(OpenSAMLMockitoRunner.class)
public class IdaResponseFromIdpUnmarshallerTest {
    @Mock
    private IdpIdaStatusUnmarshaller statusUnmarshaller;
    @Mock
    private PassthroughAssertionUnmarshaller passthroughAssertionUnmarshaller;
    @Mock
    private Response response;
    @Mock
    private Issuer issuer = null;
    private IdaResponseFromIdpUnmarshaller unmarshaller;
    private Signature signature = new SignatureBuilder().build();

    @Before
    public void setup() {
        when(response.getIssuer()).thenReturn(issuer);
        when(response.getDestination()).thenReturn("http://hello.com");
        when(response.getSignature()).thenReturn(signature);
        unmarshaller = new IdaResponseFromIdpUnmarshaller(statusUnmarshaller, passthroughAssertionUnmarshaller);
    }

    @Test
    public void transform_shouldTransformTheSamlResponseToIdaResponseByIdp() throws Exception {
        Assertion mdsAssertion = anAssertion().addAttributeStatement(anAttributeStatement().build()).buildUnencrypted();
        Assertion authnStatementAssertion = anAssertion().addAuthnStatement(anAuthnStatement().build()).buildUnencrypted();

        when(response.getAssertions()).thenReturn(newArrayList(mdsAssertion, authnStatementAssertion));
        PassthroughAssertion passthroughMdsAssertion = aPassthroughAssertion().buildMatchingDatasetAssertion();
        when(passthroughAssertionUnmarshaller.fromAssertion(mdsAssertion)).thenReturn(passthroughMdsAssertion);
        PassthroughAssertion passthroughAuthnAssertion = aPassthroughAssertion().buildAuthnStatementAssertion();
        when(passthroughAssertionUnmarshaller.fromAssertion(authnStatementAssertion)).thenReturn(passthroughAuthnAssertion);

        InboundResponseFromIdp inboundResponseFromIdp = unmarshaller.fromSaml(new ValidatedResponse(response), new ValidatedAssertions(response.getAssertions()));

        assertThat(inboundResponseFromIdp.getSignature().isPresent()).isEqualTo(true);
        assertThat(inboundResponseFromIdp.getMatchingDatasetAssertion().isPresent()).isEqualTo(true);
        assertThat(inboundResponseFromIdp.getAuthnStatementAssertion().isPresent()).isEqualTo(true);
        assertThat(inboundResponseFromIdp.getSignature().get()).isEqualTo(signature);
        assertThat(inboundResponseFromIdp.getAuthnStatementAssertion().get()).isEqualTo(passthroughAuthnAssertion);
        assertThat(inboundResponseFromIdp.getMatchingDatasetAssertion().get()).isEqualTo(passthroughMdsAssertion);
    }
}
