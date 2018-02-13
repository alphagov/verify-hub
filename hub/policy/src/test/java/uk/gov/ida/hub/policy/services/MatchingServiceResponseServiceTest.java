package uk.gov.ida.hub.policy.services;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.ServiceInfoConfigurationBuilder;
import uk.gov.ida.eventsink.EventDetailsKey;
import uk.gov.ida.eventsink.EventSinkHubEventConstants;
import uk.gov.ida.eventsink.EventSinkProxy;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseDto;
import uk.gov.ida.hub.policy.domain.EventSinkHubEvent;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;
import uk.gov.ida.hub.policy.domain.NoMatchFromMatchingService;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.UserAccountCreatedFromMatchingService;
import uk.gov.ida.hub.policy.domain.controller.WaitingForMatchingServiceResponseStateController;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
import uk.gov.ida.hub.policy.domain.state.WaitingForMatchingServiceResponseState;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.UUID;

import static java.text.MessageFormat.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceResponseServiceTest {

    @Mock
    private EventSinkProxy eventSinkProxy;
    @Mock
    private SamlEngineProxy samlEngineProxy;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;
    @Mock
    private WaitingForMatchingServiceResponseStateController waitingForMatchingServiceResponseStateController;

    private final SamlResponseDto samlResponseDto = new SamlResponseDto("saml-response");
    private final String inResponseTo = "inResponseTo";
    private final ServiceInfoConfiguration serviceInfo = ServiceInfoConfigurationBuilder.aServiceInfo().build();

    private SessionId sessionId;
    private MatchingServiceResponseService matchingServiceResponseService;

    @Before
    public void setUp() {
        matchingServiceResponseService = new MatchingServiceResponseService(eventSinkProxy, serviceInfo, samlEngineProxy, sessionRepository);
        sessionId = SessionId.createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        when(sessionRepository.getStateController(sessionId, WaitingForMatchingServiceResponseState.class)).thenReturn(waitingForMatchingServiceResponseStateController);
    }

    @Test(expected = SessionNotFoundException.class)
    public void handle_shouldThrowExceptionIfSessionDoesNotExist() throws Exception {
        when(sessionRepository.sessionExists(sessionId)).thenReturn(false);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);
    }

    @Test(expected = SessionNotFoundException.class)
    public void handle_shouldThrowExceptionIfSessionDoesNotExistInMSResponseFailureCase() throws Exception {
        when(sessionRepository.sessionExists(sessionId)).thenReturn(false);

        matchingServiceResponseService.handleFailure(sessionId);
    }

    @Test
    public void handle_shouldNotifyPolicyWhenTransformationSucceedsForAMatch() throws Exception {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.MatchingServiceMatch,
                        inResponseTo,
                        "issuer",
                        Optional.of("assertionBlob"),
                        Optional.of(LevelOfAssurance.LEVEL_2));
        when(samlEngineProxy.translateMatchingServiceResponse(samlResponseDto)).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleMatchResponseFromMatchingService(Matchers.<MatchFromMatchingService>any());
    }

    @Test
    public void handle_shouldNotifyPolicyWhenTransformationSucceedsForANoMatch() throws Exception {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                        inResponseTo,
                        "issuer",
                        Optional.<String>absent(),
                        Optional.<LevelOfAssurance>absent());
        when(samlEngineProxy.translateMatchingServiceResponse(samlResponseDto)).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleNoMatchResponseFromMatchingService(Matchers.<NoMatchFromMatchingService>any());
    }

    @Test
    public void handle_shouldNotifyPolicyWhenTransformationSucceedsForUserAccountCreated() throws Exception {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.UserAccountCreated,
                        inResponseTo,
                        "issuer",
                        Optional.of("assertionBlob"),
                        Optional.of(LevelOfAssurance.LEVEL_2));
        when(samlEngineProxy.translateMatchingServiceResponse(samlResponseDto)).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleUserAccountCreatedResponseFromMatchingService(Matchers.<UserAccountCreatedFromMatchingService>any());
    }

    @Test
    public void handle_shouldLogToEventSinkAndNotifyPolicyOnRequesterError() {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.RequesterError,
                        inResponseTo,
                        "issuer",
                        Optional.<String>absent(),
                        Optional.<LevelOfAssurance>absent());
        when(samlEngineProxy.translateMatchingServiceResponse(samlResponseDto)).thenReturn(inboundResponseFromMatchingServiceDto);
        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleRequestFailure();
        verify(eventSinkProxy, times(1)).logHubEvent(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.ERROR_EVENT);
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.message)).contains("Requester error in response from matching service for session ");
        assertThat(argumentCaptor.getValue().getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(argumentCaptor.getValue().getOriginatingService()).isEqualTo(serviceInfo.getName());
    }

    @Test
    public void handle_shouldLogToEventSinkAndUpdateStateWhenHandlingError() {
        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);

        matchingServiceResponseService.handleFailure(sessionId);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleRequestFailure();
        verify(eventSinkProxy, times(1)).logHubEvent(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.ERROR_EVENT);
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.message)).isEqualTo(format("received failure notification from saml-soap-proxy for session {0}", sessionId));
        assertThat(argumentCaptor.getValue().getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(argumentCaptor.getValue().getOriginatingService()).isEqualTo(serviceInfo.getName());
    }

    @Test
    public void handle_shouldUpdateStateWhenSamlProxyCannotProcessSaml() throws Exception {
        ArgumentCaptor<EventSinkHubEvent> argumentCaptor = ArgumentCaptor.forClass(EventSinkHubEvent.class);
        when(samlEngineProxy.translateMatchingServiceResponse(samlResponseDto)).thenThrow(ApplicationException.createAuditedException(ExceptionType.INVALID_SAML, UUID.randomUUID()));

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleRequestFailure();
        verify(eventSinkProxy, times(1)).logHubEvent(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getEventType()).isEqualTo(EventSinkHubEventConstants.EventTypes.ERROR_EVENT);
        assertThat(argumentCaptor.getValue().getDetails().get(EventDetailsKey.message)).isEqualTo("Error translating matching service response");
        assertThat(argumentCaptor.getValue().getSessionId()).isEqualTo(sessionId.getSessionId());
        assertThat(argumentCaptor.getValue().getOriginatingService()).isEqualTo(serviceInfo.getName());
    }

}
