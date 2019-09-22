package uk.gov.ida.hub.policy.services;

import uk.gov.ida.hub.policy.contracts.AbstractAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;

import javax.inject.Inject;

public class AttributeQueryService {
    private final SamlEngineProxy samlEngineProxy;
    private final SamlSoapProxyProxy samlSoapProxyProxy;

    @Inject
    public AttributeQueryService(SamlEngineProxy samlEngineProxy, SamlSoapProxyProxy samlSoapProxyProxy) {
        this.samlEngineProxy = samlEngineProxy;
        this.samlSoapProxyProxy = samlSoapProxyProxy;
    }

    public void sendAttributeQueryRequest(
        final SessionId sessionId,
        final AbstractAttributeQueryRequestDto attributeQueryRequestDto) {
        AttributeQueryContainerDto attributeQueryContainerDto = attributeQueryRequestDto.sendToSamlEngine(samlEngineProxy);
        generateAndSendMatchingServiceRequest(sessionId, attributeQueryRequestDto.isOnboarding(), attributeQueryContainerDto);
    }

    private void generateAndSendMatchingServiceRequest(
        final SessionId sessionId,
        final boolean isOnBoarding,
        final AttributeQueryContainerDto attributeQueryContainerDto) {
        AttributeQueryRequest attributeQueryRequest = new AttributeQueryRequest(
                attributeQueryContainerDto.getId(),
                attributeQueryContainerDto.getIssuer(),
                attributeQueryContainerDto.getSamlRequest(),
                attributeQueryContainerDto.getMatchingServiceUri(),
                attributeQueryContainerDto.getAttributeQueryClientTimeOut(),
                isOnBoarding,
                attributeQueryContainerDto.getCountrySignedResponse());
        samlSoapProxyProxy.sendHubMatchingServiceRequest(sessionId, attributeQueryRequest);
    }
}
