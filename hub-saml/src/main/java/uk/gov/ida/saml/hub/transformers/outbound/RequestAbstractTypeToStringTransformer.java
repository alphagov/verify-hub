package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;
import uk.gov.ida.saml.core.transformers.outbound.decorators.SamlSignatureSigner;
import uk.gov.ida.saml.hub.transformers.outbound.decorators.SigningRequestAbstractTypeSignatureCreator;

import java.util.function.Function;

public class RequestAbstractTypeToStringTransformer<TInput extends RequestAbstractType> implements Function<TInput, String> {

    private final SigningRequestAbstractTypeSignatureCreator<TInput> signatureCreator;
    private final SamlSignatureSigner<TInput> samlSignatureSigner;
    private final XmlObjectToBase64EncodedStringTransformer<TInput> xmlObjectToBase64EncodedStringTransformer;

    @Inject
    public RequestAbstractTypeToStringTransformer(
            final SigningRequestAbstractTypeSignatureCreator<TInput> signatureCreator,
            final SamlSignatureSigner<TInput> samlSignatureSigner,
            final XmlObjectToBase64EncodedStringTransformer<TInput> xmlObjectToBase64EncodedStringTransformer) {

        this.signatureCreator = signatureCreator;
        this.samlSignatureSigner = samlSignatureSigner;
        this.xmlObjectToBase64EncodedStringTransformer = xmlObjectToBase64EncodedStringTransformer;
    }

    @Override
    public String apply(final TInput input) {
        final TInput requestWithSignature = signatureCreator.addUnsignedSignatureTo(input);

        final TInput signedRequest = samlSignatureSigner.sign(requestWithSignature);

        return xmlObjectToBase64EncodedStringTransformer.apply(signedRequest);
    }

}
