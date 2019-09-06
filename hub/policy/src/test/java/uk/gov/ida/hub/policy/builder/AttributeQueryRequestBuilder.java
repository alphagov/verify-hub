package uk.gov.ida.hub.policy.builder;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class AttributeQueryRequestBuilder {

    private String requestId = UUID.randomUUID().toString();
    private String encryptedMatchingDataSetAssertion = UUID.randomUUID().toString();
    private String authnStatementAssertion = UUID.randomUUID().toString();
    private String authnRequestIssuesEntityId = UUID.randomUUID().toString();
    private URI assertionConsumerServiceUri = URI.create(UUID.randomUUID().toString());
    private String matchingServiceEntityId = UUID.randomUUID().toString();
    private DateTime matchingServiceRequestTimeOut = DateTime.now();
    private LevelOfAssurance levelOfAssurance = LevelOfAssurance.LEVEL_2;
    private PersistentId persistentId = PersistentIdBuilder.aPersistentId().build();
    private DateTime assertionExpiry = DateTime.now();
    private URI attributeQueryUri = URI.create(UUID.randomUUID().toString());
    private boolean onboarding = false;

    public static AttributeQueryRequestBuilder anAttributeQueryRequest() {
        return new AttributeQueryRequestBuilder();
    }

    public AttributeQueryRequestDto build() {
        return new AttributeQueryRequestDto(
            requestId,
            authnRequestIssuesEntityId,
            assertionConsumerServiceUri,
            assertionExpiry,
            matchingServiceEntityId,
            attributeQueryUri,
            matchingServiceRequestTimeOut,
            onboarding,
            levelOfAssurance,
            persistentId,
            Optional.empty(),
            Optional.empty(),
            encryptedMatchingDataSetAssertion,
            authnStatementAssertion
        );
    }
}

