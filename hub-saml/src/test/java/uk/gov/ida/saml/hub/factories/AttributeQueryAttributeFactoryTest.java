package uk.gov.ida.saml.hub.factories;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensaml.saml.saml2.core.Attribute;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OpenSAMLExtension.class)
public class AttributeQueryAttributeFactoryTest {

    private static AttributeQueryAttributeFactory attributeQueryAttributeFactory;

    @BeforeAll
    public static void setUp() throws Exception {
        attributeQueryAttributeFactory = new AttributeQueryAttributeFactory(new OpenSamlXmlObjectFactory());
    }

    @Test
    public void createAttribute_shouldPopulateAttributeNameFromUserAccountCreationAttributeValue(){
        UserAccountCreationAttribute userAccountCreationAttribute = UserAccountCreationAttribute.CURRENT_ADDRESS;

        Attribute attribute = attributeQueryAttributeFactory.createAttribute(userAccountCreationAttribute);

        assertThat(attribute.getName()).isEqualTo("currentaddress");
    }

    @Test
    public void createAttribute_shouldPopulateAttributeNameFormatWithUnspecifiedFormat(){
        UserAccountCreationAttribute userAccountCreationAttribute = UserAccountCreationAttribute.CURRENT_ADDRESS;

        Attribute attribute = attributeQueryAttributeFactory.createAttribute(userAccountCreationAttribute);

        assertThat(attribute.getNameFormat()).isEqualTo("urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified");
    }

    @Test
    public void createAttribute_shouldNotSetFriendlyName(){
        UserAccountCreationAttribute userAccountCreationAttribute = UserAccountCreationAttribute.CURRENT_ADDRESS;

        Attribute attribute = attributeQueryAttributeFactory.createAttribute(userAccountCreationAttribute);

        assertThat(attribute.getFriendlyName()).isNull();
    }
}
