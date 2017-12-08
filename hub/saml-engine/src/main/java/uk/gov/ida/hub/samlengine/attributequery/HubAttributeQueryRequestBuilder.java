package uk.gov.ida.hub.samlengine.attributequery;

import com.google.common.base.Optional;
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
import java.util.UUID;

public class HubAttributeQueryRequestBuilder {

    private final String hubEntityId;

    @Inject
    public HubAttributeQueryRequestBuilder(@Named("HubEntityId") String hubEntityId) {
        this.hubEntityId = hubEntityId;
    }

    public HubAttributeQueryRequest createHubAttributeQueryRequest(final AttributeQueryRequestDto attributeQueryRequestDto) {
        return new HubAttributeQueryRequest(
                attributeQueryRequestDto.getRequestId(),
                new PersistentId(attributeQueryRequestDto.getPersistentId().getNameId()),
                attributeQueryRequestDto.getEncryptedMatchingDatasetAssertion(),
                attributeQueryRequestDto.getAuthnStatementAssertion(),
                createCycle3Assertion(attributeQueryRequestDto),
                attributeQueryRequestDto.getUserAccountCreationAttributes(),
                DateTime.now(),
                attributeQueryRequestDto.getAssertionConsumerServiceUri(),
                attributeQueryRequestDto.getAuthnRequestIssuerEntityId(),
                AuthnContext.valueOf(attributeQueryRequestDto.getLevelOfAssurance().name()),
                hubEntityId);
    }

    private Optional<HubAssertion> createCycle3Assertion(AttributeQueryRequestDto attributeQueryRequestDto) {
        Optional<HubAssertion> cycle3AttributeAssertion = Optional.absent();
        if (attributeQueryRequestDto.getCycle3Dataset().isPresent()) {
            AssertionRestrictions assertionRestrictions = new AssertionRestrictions(
                    attributeQueryRequestDto.getAssertionExpiry(),
                    attributeQueryRequestDto.getRequestId(),
                    attributeQueryRequestDto.getAuthnRequestIssuerEntityId());

            Optional<Cycle3Dataset> cycle3Data = Optional.fromNullable(Cycle3Dataset.createFromData(attributeQueryRequestDto.getCycle3Dataset().get().getAttributes()));
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
