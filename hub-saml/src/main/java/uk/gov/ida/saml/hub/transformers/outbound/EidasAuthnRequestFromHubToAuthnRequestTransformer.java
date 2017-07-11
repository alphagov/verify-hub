package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class EidasAuthnRequestFromHubToAuthnRequestTransformer extends EidasAuthnRequestToAuthnRequestTransformer {

    @Inject
    public EidasAuthnRequestFromHubToAuthnRequestTransformer(OpenSamlXmlObjectFactory samlObjectFactory) {
        super(samlObjectFactory);
    }
}
