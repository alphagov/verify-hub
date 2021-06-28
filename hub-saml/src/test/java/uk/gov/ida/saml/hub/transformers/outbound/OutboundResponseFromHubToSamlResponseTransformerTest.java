package uk.gov.ida.saml.hub.transformers.outbound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Response;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.PassthroughAssertion;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.PassthroughAssertionBuilder.aPassthroughAssertion;
import static uk.gov.ida.saml.core.test.builders.ResponseBuilder.aResponse;
import static uk.gov.ida.saml.core.test.builders.ResponseForHubBuilder.anAuthnResponse;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class OutboundResponseFromHubToSamlResponseTransformerTest {
    @Mock
    private TransactionIdaStatusMarshaller statusMarshaller = null;
    @Mock
    private EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller;
    private OutboundResponseFromHubToSamlResponseTransformer transformer;

    @BeforeEach
    public void setup() {
        OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
        transformer = new OutboundResponseFromHubToSamlResponseTransformer(statusMarshaller, openSamlXmlObjectFactory, encryptedAssertionUnmarshaller);
    }

    @Test
    public void transformAssertions_shouldTransformMatchingServiceAssertions() throws Exception {
        PassthroughAssertion matchingServiceAssertion = aPassthroughAssertion().buildMatchingServiceAssertion();
        Response transformedResponse = aResponse().withNoDefaultAssertion().build();
        EncryptedAssertion transformedMatchingDatasetAssertion = anAssertion().build();
        when(encryptedAssertionUnmarshaller.transform(matchingServiceAssertion.getUnderlyingAssertionBlob())).thenReturn(transformedMatchingDatasetAssertion);

        String encryptedMatchingServiceAssertion = matchingServiceAssertion.getUnderlyingAssertionBlob();
        transformer.transformAssertions(anAuthnResponse().withEncryptedAssertions(Collections.singletonList(encryptedMatchingServiceAssertion)).buildOutboundResponseFromHub(), transformedResponse);

        assertThat(transformedResponse.getEncryptedAssertions().size()).isEqualTo(1);
        assertThat(transformedResponse.getEncryptedAssertions().get(0)).isEqualTo(transformedMatchingDatasetAssertion);
    }
}
