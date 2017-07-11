package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.AttributeValue;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class VerifiedAttributeValueBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();

    private boolean value;

    public static VerifiedAttributeValueBuilder aVerifiedValue() {
        return new VerifiedAttributeValueBuilder();
    }

    public AttributeValue build() {
        return openSamlXmlObjectFactory.createVerifiedAttributeValue(value);
    }

    public VerifiedAttributeValueBuilder withValue(boolean value) {
        this.value = value;
        return this;
    }

}
