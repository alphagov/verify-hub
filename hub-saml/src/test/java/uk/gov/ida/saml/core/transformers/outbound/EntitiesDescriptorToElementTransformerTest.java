package uk.gov.ida.saml.core.transformers.outbound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.w3c.dom.Element;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.serializers.XmlObjectToElementTransformer;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.AuthnRequestBuilder.anAuthnRequest;
import static uk.gov.ida.saml.core.test.builders.IssuerBuilder.anIssuer;

@ExtendWith(OpenSAMLExtension.class)
public class EntitiesDescriptorToElementTransformerTest {

    @Test
    public void transform_shouldTransformASamlObjectIntoAnElement() {
        AuthnRequest authnRequest = anAuthnRequest().withIssuer(anIssuer().build()).build();
        XmlObjectToElementTransformer<AuthnRequest> transformer = new XmlObjectToElementTransformer<>();

        Element result = transformer.apply(authnRequest);

        assertThat(result).isNotNull();
    }
}
