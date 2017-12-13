package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.hub.domain.EidasAuthnRequestFromHub;

import java.net.URI;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class EidasAuthnRequestFromHubToAuthnRequestTransformerTest {
    private EidasAuthnRequestFromHubToAuthnRequestTransformer transformer;
    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory;

    @Before
    public void setUp() throws Exception {
        openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
        transformer = new EidasAuthnRequestFromHubToAuthnRequestTransformer(openSamlXmlObjectFactory);
    }

    @Test
    public void shouldApplyNameIdPolicy() {
        EidasAuthnRequestFromHub request = new EidasAuthnRequestFromHub(
            "theId",
            "theIssuer",
            DateTime.now(),
            Arrays.asList(AuthnContext.LEVEL_2),
            URI.create("theUri"),
            "theProviderName"
        );

        AuthnRequest authnRequest = transformer.apply(request);

        assertThat(authnRequest.getNameIDPolicy()).isNotNull();
        assertThat(authnRequest.getNameIDPolicy().getFormat()).isEqualTo(NameIDType.PERSISTENT);
        assertThat(authnRequest.getNameIDPolicy().getAllowCreate()).isTrue();
    }
}
