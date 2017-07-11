package uk.gov.ida.saml.core.test.builders;

import javax.xml.namespace.QName;

import org.opensaml.saml.saml2.core.Attribute;

import com.google.common.base.Optional;

import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.extensions.IdpFraudEventId;
import uk.gov.ida.saml.core.extensions.StringValueSamlObject;
import uk.gov.ida.saml.core.extensions.impl.StringBasedMdsAttributeValueBuilder;

public class IdpFraudEventIdAttributeBuilder {

    private static final java.lang.String INVALID_TYPE_LOCAL_NAME = "InvalidFraudEventType";
    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private Optional<String> value = Optional.fromNullable("default-event-id");

    public static IdpFraudEventIdAttributeBuilder anIdpFraudEventIdAttribute() {
        return new IdpFraudEventIdAttributeBuilder();
    }

    public Attribute build() {
        Attribute attribute = openSamlXmlObjectFactory.createAttribute();
        attribute.setName(IdaConstants.Attributes_1_1.IdpFraudEventId.NAME);
        if (value.isPresent()){
            IdpFraudEventId attributeValue = openSamlXmlObjectFactory.createIdpFraudEventAttributeValue(value.get());
            attribute.getAttributeValues().add(attributeValue);
        }

        return attribute;
    }

    public Attribute buildInvalidAttribute() {
        Attribute attribute = openSamlXmlObjectFactory.createAttribute();
        attribute.setName(IdaConstants.Attributes_1_1.IdpFraudEventId.NAME);
        if (value.isPresent()){
            QName typeName = new QName(IdaConstants.IDA_NS, INVALID_TYPE_LOCAL_NAME, IdaConstants.IDA_PREFIX);
            StringValueSamlObject idpFraudEventId = new StringBasedMdsAttributeValueBuilder().buildObject(IdpFraudEventId.DEFAULT_ELEMENT_NAME, typeName);
            idpFraudEventId.setValue(value.get());
            attribute.getAttributeValues().add(idpFraudEventId);
        }

        return attribute;
    }

    public IdpFraudEventIdAttributeBuilder withValue(String value){
        this.value = Optional.fromNullable(value);
        return this;
    }
}
