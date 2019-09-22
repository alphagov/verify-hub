package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.builder.AttributeQueryRequestBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.contracts.AbstractAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    private AttributeQueryService service;
    private SessionId sessionId;

    @Before
    public void setup() {
        sessionId = SessionIdBuilder.aSessionId().build();
        service = new AttributeQueryService(samlEngineProxy, samlSoapProxyProxy);
    }

    @Test
    public void shouldGenerateAttributeQueryAndSendRequestToMatchingService() {
        // Given
        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        AttributeQueryContainerDto build = anAttributeQueryContainerDto().build();
        when(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto)).thenReturn(build);

        // When
        service.sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);

        // Then
        verify(samlEngineProxy).generateAttributeQuery(attributeQueryRequestDto);
        verify(samlSoapProxyProxy).sendHubMatchingServiceRequest(eq(sessionId), any());
    }

    @Test(expected = ApplicationException.class)
    public void shouldPropagateExceptionThrownBySamlEngineAndNotSendAttributeQuery() {
        // Given
        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        when(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto))
                .thenThrow(ApplicationException.createAuditedException(INVALID_SAML, UUID.randomUUID()));

        // When
        service.sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);

        // Then
        verify(samlEngineProxy, times(1)).generateAttributeQuery(attributeQueryRequestDto);
        verify(samlSoapProxyProxy, times(0)).sendHubMatchingServiceRequest(eq(sessionId), any());
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
            eidasAttributeQueryRequestDto.isOnboarding(),
            eidasAttributeQueryRequestDto.getCountrySignedResponse()
        );
        when(samlEngineProxy.generateEidasAttributeQuery(eidasAttributeQueryRequestDto)).thenReturn(attributeQueryContainerDto);

        service.sendAttributeQueryRequest(sessionId, eidasAttributeQueryRequestDto);

        verify(samlEngineProxy).generateEidasAttributeQuery(eidasAttributeQueryRequestDto);
        verify(samlSoapProxyProxy).sendHubMatchingServiceRequest(sessionId, attributeQueryRequest);
    }

    @Test
    public void shouldSendOptionalCountrySignedResponseContainerToSamlSoapService() {
        SessionId sessionId = mock(SessionId.class);
        AbstractAttributeQueryRequestDto attributeQueryRequestDto = mock(AbstractAttributeQueryRequestDto.class);
        AttributeQueryContainerDto attributeQueryContainerDto = mock(AttributeQueryContainerDto.class);
        CountrySignedResponseContainer countrySignedResponseContainer = mock(CountrySignedResponseContainer.class);
        ArgumentCaptor<AttributeQueryRequest> argumentCaptor = ArgumentCaptor.forClass(AttributeQueryRequest.class);
        when(attributeQueryRequestDto.sendToSamlEngine(samlEngineProxy)).thenReturn(attributeQueryContainerDto);
        when(attributeQueryContainerDto.getCountrySignedResponse()).thenReturn(Optional.of(countrySignedResponseContainer));
        service.sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);
        verify(attributeQueryContainerDto).getCountrySignedResponse();
        verify(samlSoapProxyProxy).sendHubMatchingServiceRequest(eq(sessionId), argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getCountrySignedResponse().get()).isEqualTo(countrySignedResponseContainer);
    }
}
