package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.builder.AttributeQueryRequestBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.SuccessFromIdp;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectedStateController;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;

import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.ExceptionType.INVALID_SAML;
import static uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;
import static uk.gov.ida.hub.policy.builder.EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto;

@RunWith(MockitoJUnitRunner.class)
public class AttributeQueryServiceTest {
    @Mock
    private SamlEngineProxy samlEngineProxy;

    @Mock
    private SamlSoapProxyProxy samlSoapProxyProxy;

    @Mock
    private IdpSelectedStateController idpSelectedStateController;

    @Mock
    private SessionRepository sessionRepository;

    private AttributeQueryService service;
    private SessionId sessionId;

    @Before
    public void setup() {
        sessionId = SessionIdBuilder.aSessionId().build();
        service = new AttributeQueryService(samlEngineProxy, samlSoapProxyProxy);
        when(sessionRepository.getStateController(sessionId, IdpSelectedStateTransitional.class)).thenReturn(idpSelectedStateController);
    }

    @Test
    public void shouldGenerateAttributeQueryAndSendRequestToMatchingService() throws Exception {
        // Given
        boolean isRegistration = true;
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(isRegistration);
        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        stub(idpSelectedStateController.createAttributeQuery(any(SuccessFromIdp.class))).toReturn(attributeQueryRequestDto);
        AttributeQueryContainerDto build = anAttributeQueryContainerDto().build();
        stub(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto)).toReturn(build);

        // When
        service.sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);

        // Then
        verify(samlEngineProxy).generateAttributeQuery(attributeQueryRequestDto);
        verify(samlSoapProxyProxy).sendHubMatchingServiceRequest(eq(sessionId), Matchers.<AttributeQueryRequest>any());
    }

    @Test(expected = ApplicationException.class)
    public void shouldPropagateExceptionThrownBySamlEngineAndNotSendAttributeQuery() throws Exception {
        // Given
        boolean isRegistration = true;
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(isRegistration);
        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        stub(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto)).toThrow(ApplicationException.createAuditedException(INVALID_SAML, UUID.randomUUID()));

        // When
        service.sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);

        // Then
        verify(samlEngineProxy, times(1)).generateAttributeQuery(attributeQueryRequestDto);
        verify(samlSoapProxyProxy, times(0)).sendHubMatchingServiceRequest(eq(sessionId), Matchers.<AttributeQueryRequest>any());
    }

    @Test
    public void shouldGenerateEidasAttributeQueryAndSendRequestToMatchingService() {
        final EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = anEidasAttributeQueryRequestDto().build();
        final AttributeQueryContainerDto attributeQueryContainerDto = anAttributeQueryContainerDto().build();
        final AttributeQueryRequest attributeQueryRequest = new AttributeQueryRequest(
            attributeQueryContainerDto.getId(),
            attributeQueryContainerDto.getIssuer(),
            attributeQueryContainerDto.getSamlRequest(),
            attributeQueryContainerDto.getMatchingServiceUri(),
            attributeQueryContainerDto.getAttributeQueryClientTimeOut(),
            eidasAttributeQueryRequestDto.isOnboarding()
        );
        when(samlEngineProxy.generateEidasAttributeQuery(eidasAttributeQueryRequestDto)).thenReturn(attributeQueryContainerDto);

        service.sendAttributeQueryRequest(sessionId, eidasAttributeQueryRequestDto);

        verify(samlEngineProxy).generateEidasAttributeQuery(eidasAttributeQueryRequestDto);
        verify(samlSoapProxyProxy).sendHubMatchingServiceRequest(sessionId, attributeQueryRequest);
    }
}
