package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.slf4j.event.Level;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import java.util.List;

public class KeyDescriptorFinder {

    public KeyDescriptor find(
            List<KeyDescriptor> keyDescriptors,
            UsageType usageType,
            String entityId) {

        return keyDescriptors.stream()
                .filter(keyDescriptor -> keyDescriptor.getUse().equals(usageType))
                .filter(keyDescriptor -> (keyDescriptor.getKeyInfo().getKeyNames().isEmpty() || entityId == null || keyDescriptor.getKeyInfo().getKeyNames().get(0).getValue().equals(entityId)))
                .findFirst()
                .orElseThrow(() -> throwError(usageType, entityId));
    }

    private SamlTransformationErrorException throwError(UsageType usageType, String entityId) {
        SamlValidationSpecificationFailure failure = SamlTransformationErrorFactory.missingKey(usageType.toString(), entityId);
        return new SamlTransformationErrorException(failure.getErrorMessage(), Level.ERROR);
    }
}