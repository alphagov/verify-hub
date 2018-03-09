package uk.gov.ida.hub.samlengine.attributequery;

import org.joda.time.DateTime;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.saml.core.domain.AssertionRestrictions;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.Cycle3Dataset;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.UUID;

public class HubAttributeQueryRequestBuilder {

    private final String hubEntityId;

    @Inject
    public HubAttributeQueryRequestBuilder(@Named("HubEntityId") String hubEntityId) {
        this.hubEntityId = hubEntityId;
    }

    public HubAttributeQueryRequest createHubAttributeQueryRequest(final AttributeQueryRequestDto attributeQueryRequestDto) {
        //TODO: Updating verify-hub to use the new verify-hub-saml lib, but seems like some of the classes in saml-utils were
        //updated to use Java Utils Optional but objects in ida-hub-saml were not. So we now have a mixture of classes
        //This doesn't relate to our current update - will come back and fix this in later updates.
        com.google.common.base.Optional<HubAssertion> cycle3AttributeAssertion = com.google.common.base.Optional.fromJavaUtil(createCycle3Assertion(attributeQueryRequestDto));

        return new HubAttributeQueryRequest(
                attributeQueryRequestDto.getRequestId(),
                new PersistentId(attributeQueryRequestDto.getPersistentId().getNameId()),
                attributeQueryRequestDto.getEncryptedMatchingDatasetAssertion(),
                attributeQueryRequestDto.getAuthnStatementAssertion(),
                cycle3AttributeAssertion,
                attributeQueryRequestDto.getUserAccountCreationAttributes(),
                DateTime.now(),
                attributeQueryRequestDto.getAssertionConsumerServiceUri(),
                attributeQueryRequestDto.getAuthnRequestIssuerEntityId(),
                AuthnContext.valueOf(attributeQueryRequestDto.getLevelOfAssurance().name()),
                hubEntityId);
    }

    private Optional<HubAssertion> createCycle3Assertion(AttributeQueryRequestDto attributeQueryRequestDto) {
        Optional<HubAssertion> cycle3AttributeAssertion = Optional.empty();
        if (attributeQueryRequestDto.getCycle3Dataset().isPresent()) {
            AssertionRestrictions assertionRestrictions = new AssertionRestrictions(
                    attributeQueryRequestDto.getAssertionExpiry(),
                    attributeQueryRequestDto.getRequestId(),
                    attributeQueryRequestDto.getAuthnRequestIssuerEntityId());

            Optional<Cycle3Dataset> cycle3Data = Optional.of(Cycle3Dataset.createFromData(attributeQueryRequestDto.getCycle3Dataset().get().getAttributes()));
            cycle3AttributeAssertion = Optional.of(new HubAssertion(
                    UUID.randomUUID().toString(),
                    hubEntityId,
                    DateTime.now(),
                    new PersistentId(attributeQueryRequestDto.getPersistentId().getNameId()),
                    assertionRestrictions,
                    cycle3Data
            ));
        }
        return cycle3AttributeAssertion;
    }
}
