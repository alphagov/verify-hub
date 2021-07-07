package uk.gov.ida.hub.policy.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.builder.AttributeQueryRequestBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.common.ExceptionType.INVALID_SAML;
import static uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto;

@ExtendWith(MockitoExtension.class)
public class AttributeQueryServiceTest {
    @Mock
    private SamlEngineProxy samlEngineProxy;

    @Mock
    private SamlSoapProxyProxy samlSoapProxyProxy;

    private AttributeQueryService service;
    private SessionId sessionId;

    @BeforeEach
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

    @Test
    public void shouldPropagateExceptionThrownBySamlEngineAndNotSendAttributeQuery() {
        Assertions.assertThrows(ApplicationException.class, () -> {
            // Given
            AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
            when(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto))
                    .thenThrow(ApplicationException.createAuditedException(INVALID_SAML, UUID.randomUUID()));

            // When
            service.sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);

            // Then
            verify(samlEngineProxy, times(1)).generateAttributeQuery(attributeQueryRequestDto);
            verify(samlSoapProxyProxy, times(0)).sendHubMatchingServiceRequest(eq(sessionId), any());

        });
    }
}
