package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.AttributeQueryRequestBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.Cycle3AttributeRequestData;
import uk.gov.ida.hub.policy.domain.Cycle3Dataset;
import uk.gov.ida.hub.policy.domain.Cycle3UserInput;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.AwaitingCycle3DataStateController;
import uk.gov.ida.hub.policy.domain.state.AbstractAwaitingCycle3DataState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class Cycle3ServiceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private AwaitingCycle3DataStateController awaitingCycle3DataStateController;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AttributeQueryService attributeQueryService;

    private static final Cycle3AttributeRequestData ATTRIBUTE_REQUEST_DATA = new Cycle3AttributeRequestData("attribute-name", "issuer-id");
    private Cycle3Service service;
    private Cycle3UserInput cycle3UserInput;
    private SessionId sessionId;

    @Before
    public void setup() {
        sessionId = SessionIdBuilder.aSessionId().build();
        cycle3UserInput = new Cycle3UserInput("test-value", "principal-ip-address-as-seen-by-hub");
        service = new Cycle3Service(sessionRepository, attributeQueryService);
        when(sessionRepository.getStateController(sessionId, AbstractAwaitingCycle3DataState.class)).thenReturn(awaitingCycle3DataStateController);

    }

    @Test
    public void shouldSendRequestToMatchingServiceViaAttributeQueryServiceAndUpdateSessionStateWhenSuccessfulResponseIsReceived() {
        // Given
        Cycle3AttributeRequestData attributeRequestData = new Cycle3AttributeRequestData("attribute-name", "issuer-id");
        when(awaitingCycle3DataStateController.getCycle3AttributeRequestData())
                .thenReturn(attributeRequestData);

        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        when(awaitingCycle3DataStateController.createAttributeQuery(any(Cycle3Dataset.class)))
                .thenReturn(attributeQueryRequestDto);

        // When
        service.sendCycle3MatchingRequest(sessionId, cycle3UserInput);

        // Then
        verify(awaitingCycle3DataStateController).createAttributeQuery(any(Cycle3Dataset.class));
        verify(attributeQueryService).sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);
        verify(awaitingCycle3DataStateController).handleCycle3DataSubmitted("principal-ip-address-as-seen-by-hub");
    }

    @Test
    public void shouldReturnCycle3AttributeRequestDataAfterReceivingCycle3AttributeRequestDataForVerifyFlow() {
        // Given
        when(awaitingCycle3DataStateController.getCycle3AttributeRequestData()).thenReturn(ATTRIBUTE_REQUEST_DATA);

        // When
        Cycle3AttributeRequestData result = service.getCycle3AttributeRequestData(sessionId);

        // Then
        assertThat(result).isEqualTo(ATTRIBUTE_REQUEST_DATA);
    }

}
