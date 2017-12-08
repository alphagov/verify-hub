package uk.gov.ida.hub.samlengine.attributequery;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.saml.core.domain.AssertionRestrictions;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.domain.Cycle3Dataset;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

public class HubEidasAttributeQueryRequestBuilder {

    private final String hubEntityId;

    @Inject
    public HubEidasAttributeQueryRequestBuilder(@Named("HubEntityId") String hubEntityId) {
        this.hubEntityId = hubEntityId;
    }

    public HubEidasAttributeQueryRequest createHubAttributeQueryRequest(EidasAttributeQueryRequestDto dto) {
        return new HubEidasAttributeQueryRequest(
            dto.getRequestId(),
            hubEntityId,
            DateTime.now(),
            new PersistentId(dto.getPersistentId().getNameId()),
            dto.getAssertionConsumerServiceUri(),
            dto.getAuthnRequestIssuerEntityId(),
            dto.getEncryptedIdentityAssertion(),
            AuthnContext.valueOf(dto.getLevelOfAssurance().name()),
            createCycle3Assertion(dto).transform(java.util.Optional::of).or(java.util.Optional::empty),
            dto.getUserAccountCreationAttributes().transform(java.util.Optional::of).or(java.util.Optional::empty));
    }

    private Optional<HubAssertion> createCycle3Assertion(EidasAttributeQueryRequestDto attributeQueryRequestDto) {
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
                com.google.common.base.Optional.fromNullable(cycle3Data.get())
            ));
        }
        return cycle3AttributeAssertion;
    }
}
