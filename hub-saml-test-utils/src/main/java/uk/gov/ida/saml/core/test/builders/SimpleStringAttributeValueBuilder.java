package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.AttributeValue;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

public class SimpleStringAttributeValueBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();

    private String value;

    public static SimpleStringAttributeValueBuilder aSimpleStringValue() {
        return new SimpleStringAttributeValueBuilder();
    }

    public AttributeValue build() {
        return openSamlXmlObjectFactory.createSimpleMdsAttributeValue(value);
    }

    public SimpleStringAttributeValueBuilder withValue(String value) {
        this.value = value;
        return this;
    }

}
