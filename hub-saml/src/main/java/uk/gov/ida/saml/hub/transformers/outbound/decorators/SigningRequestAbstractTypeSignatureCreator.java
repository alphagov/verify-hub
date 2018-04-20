package uk.gov.ida.saml.hub.transformers.outbound.decorators;

import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.xmlsec.signature.Signature;
import uk.gov.ida.saml.security.SignatureFactory;

public class SigningRequestAbstractTypeSignatureCreator<T extends RequestAbstractType> {
    private final SignatureFactory signatureFactory;

    public SigningRequestAbstractTypeSignatureCreator(SignatureFactory signatureFactory) {
        this.signatureFactory = signatureFactory;
    }

    public T addUnsignedSignatureTo(T requestAbstractType) {
        Signature signature = signatureFactory.createSignature(requestAbstractType.getID());
        requestAbstractType.setSignature(signature);
        return requestAbstractType;
    }
}
