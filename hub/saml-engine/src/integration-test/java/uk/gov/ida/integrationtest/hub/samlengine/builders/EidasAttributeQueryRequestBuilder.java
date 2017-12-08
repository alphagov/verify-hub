package uk.gov.ida.integrationtest.hub.samlengine.builders;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.util.UUID;

import static uk.gov.ida.hub.samlengine.builders.EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto;

public class EidasAttributeQueryRequestBuilder {

    public EidasAttributeQueryRequestDto build() {

        XmlObjectToBase64EncodedStringTransformer<XMLObject> toBase64EncodedStringTransformer = new XmlObjectToBase64EncodedStringTransformer<>();
        EncryptedAssertion encryptedIdentityAssertion = AssertionBuilder.anAssertion().withId(UUID.randomUUID().toString()).build();
        String encryptedIdentityAssertionString = toBase64EncodedStringTransformer.apply(encryptedIdentityAssertion);

        return anEidasAttributeQueryRequestDto().withEncryptedIdentityAssertion(encryptedIdentityAssertionString).build();
    }
}
