package uk.gov.ida.integrationtest.hub.samlengine.builders;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.hub.samlengine.domain.PersistentId;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.serializers.XmlObjectToBase64EncodedStringTransformer;

import java.util.UUID;

import static uk.gov.ida.hub.samlengine.builders.HubMatchingServiceRequestDtoBuilder.aHubMatchingServiceRequestDto;
import static uk.gov.ida.hub.samlengine.builders.PersistentIdBuilder.aPersistentId;

public class AttributeQueryRequestBuilder {

    public AttributeQueryRequestDto build() {
        return build(buildUID(), buildUID(), buildUID(), buildUID());
    }

    public AttributeQueryRequestDto build(String persistentIdName, String matchingDatasetAssertionId, String
            authnStatementAssertionId, String requestId) {
        XmlObjectToBase64EncodedStringTransformer<XMLObject> toBase64EncodedStringTransformer = new XmlObjectToBase64EncodedStringTransformer<>();
        final PersistentId persistentId = aPersistentId().withNameId(persistentIdName).buildSamlEnginePersistentId();
        EncryptedAssertion encryptedAuthnAssertion = AssertionBuilder.anAssertion().withId(authnStatementAssertionId).build();
        String encryptedAuthnAssertionString = toBase64EncodedStringTransformer.apply(encryptedAuthnAssertion);
        EncryptedAssertion encryptedMdsAssertion = AssertionBuilder.anAssertion().withId(matchingDatasetAssertionId).build();
        String encryptedMdsAssertionString = toBase64EncodedStringTransformer.apply(encryptedMdsAssertion);


        return aHubMatchingServiceRequestDto()
                .withId(requestId)
                .withMatchingServiceEntityId(TestEntityIds.TEST_RP_MS)
                .withPersistentId(persistentId)
                .withEncryptedMatchingDatasetAssertion(encryptedMdsAssertionString)
                .withEncryptedAuthnAssertion(encryptedAuthnAssertionString)
                .build();
    }

    private String buildUID() {
        return UUID.randomUUID().toString();
    }
}
