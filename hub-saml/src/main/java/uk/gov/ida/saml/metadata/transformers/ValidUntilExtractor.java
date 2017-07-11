package uk.gov.ida.saml.metadata.transformers;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

public class ValidUntilExtractor {

    DateTime extract(EntityDescriptor entityDescriptor) {
        DateTime validUntil = entityDescriptor.getValidUntil();
        DateTime expires;

        if (validUntil != null) {
            expires = validUntil;
        } else {
            expires = DateTime.now().plus(entityDescriptor.getCacheDuration());
        }
        return expires;
    }
}
