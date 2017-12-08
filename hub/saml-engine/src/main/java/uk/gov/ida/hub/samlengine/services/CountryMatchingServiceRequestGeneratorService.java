package uk.gov.ida.hub.samlengine.services;

import uk.gov.ida.hub.samlengine.attributequery.AttributeQueryGenerator;
import uk.gov.ida.hub.samlengine.attributequery.HubEidasAttributeQueryRequestBuilder;
import uk.gov.ida.hub.samlengine.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlengine.domain.EidasAttributeQueryRequestDto;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;

import javax.inject.Inject;

public class CountryMatchingServiceRequestGeneratorService {

    private final HubEidasAttributeQueryRequestBuilder eidasAttributeQueryRequestBuilder;
    private final AttributeQueryGenerator<HubEidasAttributeQueryRequest> eidasAttributeQueryGenerator;

    @Inject
    public CountryMatchingServiceRequestGeneratorService(
        HubEidasAttributeQueryRequestBuilder eidasAttributeQueryRequestBuilder,
        AttributeQueryGenerator<HubEidasAttributeQueryRequest> eidasAttributeQueryGenerator) {
        this.eidasAttributeQueryRequestBuilder = eidasAttributeQueryRequestBuilder;
        this.eidasAttributeQueryGenerator = eidasAttributeQueryGenerator;
    }

    public AttributeQueryContainerDto generate(EidasAttributeQueryRequestDto dto) {
      HubEidasAttributeQueryRequest hubEidasAttributeQueryRequest = eidasAttributeQueryRequestBuilder.createHubAttributeQueryRequest(dto);
      return eidasAttributeQueryGenerator.newCreateAttributeQueryContainer(
          hubEidasAttributeQueryRequest,
          dto.getAttributeQueryUri(),
          dto.getMatchingServiceEntityId(),
          dto.getMatchingServiceRequestTimeOut(),
          dto.isOnboarding());
    }
}
