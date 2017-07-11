package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
public class KeyDescriptorMarshaller {

    public Certificate toCertificate(KeyDescriptor keyDescriptor) {
        String entityId = null;
        if (!keyDescriptor.getKeyInfo().getKeyNames().isEmpty()) {
            entityId = keyDescriptor.getKeyInfo().getKeyNames().get(0).getValue();
        }
        return transformCertificate(entityId, keyDescriptor);
    }

    private Certificate transformCertificate(String entityId, KeyDescriptor keyDescriptor) {
        String x509Certificate = keyDescriptor.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue();
        final Certificate.KeyUse keyUse = transformUsageType(keyDescriptor.getUse());
        return new Certificate(entityId, x509Certificate, keyUse);
    }

    private Certificate.KeyUse transformUsageType(UsageType usageType) {

        switch (usageType) {
            case ENCRYPTION:
                return Certificate.KeyUse.Encryption;
            case SIGNING:
                return Certificate.KeyUse.Signing;
            case UNSPECIFIED:
                SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.unsupportedKey(usageType.toString());
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            default:
                throw new IllegalArgumentException("SamlObjectParser will have failed before reaching here.");
        }
    }
}
