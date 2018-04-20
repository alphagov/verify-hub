package uk.gov.ida.saml.hub.transformers.outbound;

import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;

public class AssertionFromIdpToAssertionTransformer {

    private final StringToOpenSamlObjectTransformer<Assertion> stringAssertionTransformer;

    public AssertionFromIdpToAssertionTransformer(StringToOpenSamlObjectTransformer<Assertion> stringAssertionTransformer) {
        this.stringAssertionTransformer = stringAssertionTransformer;
    }

    public Assertion transform(String assertionString) {
        Assertion assertion = stringAssertionTransformer.apply(assertionString);
        assertion.detach();
        return assertion;
    }
}
