package uk.gov.ida.hub.samlengine.services;

import uk.gov.ida.hub.samlengine.attributequery.AttributeQueryGenerator;
import uk.gov.ida.hub.samlengine.attributequery.HubAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.AttributeQueryRequestDto;
import uk.gov.ida.saml.hub.domain.HubAttributeQueryRequest;

import javax.inject.Inject;

public class MatchingServiceRequestGeneratorService {

    private final AttributeQueryGenerator<HubAttributeQueryRequest> attributeQueryGenerator;
    private final HubAttributeQueryRequestBuilder hubAttributeQueryRequestBuilder;

    @Inject
    public MatchingServiceRequestGeneratorService(AttributeQueryGenerator<HubAttributeQueryRequest> attributeQueryGenerator,
                                                  HubAttributeQueryRequestBuilder hubAttributeQueryRequestBuilder) {
        this.attributeQueryGenerator = attributeQueryGenerator;
        this.hubAttributeQueryRequestBuilder = hubAttributeQueryRequestBuilder;
    }

    public AttributeQueryContainerDto generate(AttributeQueryRequestDto dto) {
        HubAttributeQueryRequest hubAttributeQueryRequest = hubAttributeQueryRequestBuilder.createHubAttributeQueryRequest(dto);
        return attributeQueryGenerator.newCreateAttributeQueryContainer(hubAttributeQueryRequest,
                dto.getAttributeQueryUri(),
                dto.getMatchingServiceEntityId(),
                dto.getMatchingServiceRequestTimeOut(),
                dto.isOnboarding());
    }
}
