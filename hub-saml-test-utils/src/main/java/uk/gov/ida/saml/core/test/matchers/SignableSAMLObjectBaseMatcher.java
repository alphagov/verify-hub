package uk.gov.ida.saml.core.test.matchers;

import org.assertj.core.api.Condition;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.RequestAbstractType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import uk.gov.ida.saml.core.test.TestCredentialFactory;
import uk.gov.ida.saml.core.test.validators.SingleCertificateSignatureValidator;
import uk.gov.ida.saml.security.SamlMessageSignatureValidator;
import uk.gov.ida.saml.security.SignatureValidator;

public class SignableSAMLObjectBaseMatcher extends Condition<SignableSAMLObject> {

    private final SamlMessageSignatureValidator samlMessageSignatureValidator;

    public SignableSAMLObjectBaseMatcher(SamlMessageSignatureValidator samlMessageSignatureValidator) {
        this.samlMessageSignatureValidator = samlMessageSignatureValidator;
    }

    public static SignableSAMLObjectBaseMatcher signedBy(String publicCert, String privateKey) {
        final SignatureValidator signatureValidator = new SingleCertificateSignatureValidator(
                new TestCredentialFactory(publicCert, privateKey).getSigningCredential()
        );
        return new SignableSAMLObjectBaseMatcher(new SamlMessageSignatureValidator(signatureValidator));
    }

    @Override
    public boolean matches(SignableSAMLObject value) {
        if (value instanceof Response) {
            return samlMessageSignatureValidator.validate((Response) value, IDPSSODescriptor.DEFAULT_ELEMENT_NAME).isOK();
        }
        else if (value instanceof RequestAbstractType) {
            return samlMessageSignatureValidator.validate((RequestAbstractType) value, SPSSODescriptor.DEFAULT_ELEMENT_NAME).isOK();
        }
        else if (value instanceof Assertion) {
            return samlMessageSignatureValidator.validate((Assertion) value, IDPSSODescriptor.DEFAULT_ELEMENT_NAME).isOK();
        }
        else {
            throw new IllegalArgumentException("Don't know how to validate signature of a " + value.getClass());
        }
    }
}
