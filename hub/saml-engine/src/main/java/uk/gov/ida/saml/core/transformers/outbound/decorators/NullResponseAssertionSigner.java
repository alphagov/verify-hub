package uk.gov.ida.saml.core.transformers.outbound.decorators;

import org.opensaml.saml.saml2.core.Response;

public class NullResponseAssertionSigner extends ResponseAssertionSigner {
    public NullResponseAssertionSigner() {
        super(null);
    }

    @Override
    public Response signAssertions(Response response) {
        return response;
    }
}
