package uk.gov.ida.hub.samlengine.services;

import org.joda.time.DateTime;
import uk.gov.ida.hub.samlengine.attributequery.AttributeQueryGenerator;
import uk.gov.ida.hub.samlengine.contracts.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlengine.domain.SamlMessageDto;
import uk.gov.ida.saml.core.domain.PersistentId;
import uk.gov.ida.saml.hub.domain.MatchingServiceHealthCheckRequest;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.text.MessageFormat;
import java.util.UUID;

public class MatchingServiceHealthcheckRequestGeneratorService {

    private final AttributeQueryGenerator<MatchingServiceHealthCheckRequest> attributeQueryGenerator;
    private final String hubEntityId;

    @Inject
    public MatchingServiceHealthcheckRequestGeneratorService(AttributeQueryGenerator<MatchingServiceHealthCheckRequest> attributeQueryGenerator,
                                                             @Named("HubEntityId") String hubEntityId) {
        this.attributeQueryGenerator = attributeQueryGenerator;
        this.hubEntityId = hubEntityId;
    }

    public SamlMessageDto generate(MatchingServiceHealthCheckerRequestDto dto) {
        MatchingServiceHealthCheckRequest matchingServiceRequest = new MatchingServiceHealthCheckRequest(
                MessageFormat.format("healthcheck-request-{0}", UUID.randomUUID().toString()),
                DateTime.now(),
                new PersistentId("healthcheck-pid"),
                URI.create(""),
                dto.getTransactionEntityId(),
                hubEntityId
        );

        return attributeQueryGenerator.createAttributeQueryContainer(matchingServiceRequest, dto.getMatchingServiceEntityId());
    }
}
