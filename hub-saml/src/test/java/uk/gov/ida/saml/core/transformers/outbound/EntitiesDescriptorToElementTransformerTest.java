package uk.gov.ida.saml.core.transformers.outbound;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.w3c.dom.Element;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;

@RunWith(OpenSAMLRunner.class)
public class EntitiesDescriptorToElementTransformerTest {

    @Test
    public void transform_shouldTransformASamlObjectIntoAnElement() throws Exception {
        AuthnRequest authnRequest = anAuthnRequest().withIssuer(anIssuer().build()).build();
        XmlObjectToElementTransformer<AuthnRequest> transformer = new XmlObjectToElementTransformer<>();

        Element result = transformer.apply(authnRequest);

        assertThat(result).isNotNull();
    }
}
