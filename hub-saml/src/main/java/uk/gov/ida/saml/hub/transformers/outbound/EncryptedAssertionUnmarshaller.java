package uk.gov.ida.saml.hub.transformers.outbound;


import org.opensaml.saml.saml2.core.EncryptedAssertion;
import uk.gov.ida.saml.deserializers.StringToOpenSamlObjectTransformer;

public class EncryptedAssertionUnmarshaller {
    private final StringToOpenSamlObjectTransformer<EncryptedAssertion> stringAssertionTransformer;

    public EncryptedAssertionUnmarshaller(StringToOpenSamlObjectTransformer<EncryptedAssertion> stringAssertionTransformer) {
        this.stringAssertionTransformer = stringAssertionTransformer;
    }

    public EncryptedAssertion transform(String assertionString) {
        EncryptedAssertion assertion = stringAssertionTransformer.apply(assertionString);
        assertion.detach();
        return assertion;
    }
}
