package uk.gov.ida.hub.policy.services;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.exceptions.ApplicationException;
import uk.gov.ida.hub.policy.contracts.InboundResponseFromMatchingServiceDto;
import uk.gov.ida.hub.policy.contracts.SamlResponseDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingServiceIdaStatus;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.WaitingForMatchingServiceResponseStateController;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundException;
import uk.gov.ida.hub.policy.domain.state.WaitingForMatchingServiceResponseState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import java.util.UUID;

import static java.text.MessageFormat.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;

@RunWith(MockitoJUnitRunner.class)
public class MatchingServiceResponseServiceTest {

    @Mock
    private HubEventLogger eventLogger;
    @Mock
    private SamlEngineProxy samlEngineProxy;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private WaitingForMatchingServiceResponseStateController waitingForMatchingServiceResponseStateController;

    private final SamlResponseDto samlResponseDto = new SamlResponseDto("saml-response");
    private final String inResponseTo = "inResponseTo";

    private SessionId sessionId;
    private MatchingServiceResponseService matchingServiceResponseService;

    @Before
    public void setUp() {
        matchingServiceResponseService = new MatchingServiceResponseService(samlEngineProxy, sessionRepository, eventLogger);
        sessionId = SessionId.createNewSessionId();
        when(sessionRepository.sessionExists(sessionId)).thenReturn(true);
        when(sessionRepository.getStateController(sessionId, WaitingForMatchingServiceResponseState.class)).thenReturn(waitingForMatchingServiceResponseStateController);
        when(sessionRepository.getRequestIssuerEntityId(sessionId)).thenReturn(TEST_RP);
    }

    @Test(expected = SessionNotFoundException.class)
    public void handle_shouldThrowExceptionIfSessionDoesNotExist() {
        when(sessionRepository.sessionExists(sessionId)).thenReturn(false);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);
    }

    @Test(expected = SessionNotFoundException.class)
    public void handle_shouldThrowExceptionIfSessionDoesNotExistInMSResponseFailureCase() {
        when(sessionRepository.sessionExists(sessionId)).thenReturn(false);

        matchingServiceResponseService.handleFailure(sessionId);
    }

    @Test
    public void handle_shouldNotifyPolicyWhenTransformationSucceedsForAMatch() {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.MatchingServiceMatch,
                        inResponseTo,
                        "issuer",
                        Optional.of("assertionBlob"),
                        Optional.of(LevelOfAssurance.LEVEL_2));
        when(samlEngineProxy.translateMatchingServiceResponse(any())).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleMatchResponseFromMatchingService(any());
    }

    @Test
    public void handle_shouldNotifyPolicyWhenTransformationSucceedsForANoMatch() {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.NoMatchingServiceMatchFromMatchingService,
                        inResponseTo,
                        "issuer",
                        Optional.<String>absent(),
                        Optional.<LevelOfAssurance>absent());
        when(samlEngineProxy.translateMatchingServiceResponse(any())).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleNoMatchResponseFromMatchingService(any());
    }

    @Test
    public void handle_shouldNotifyPolicyWhenTransformationSucceedsForUserAccountCreated() {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.UserAccountCreated,
                        inResponseTo,
                        "issuer",
                        Optional.of("assertionBlob"),
                        Optional.of(LevelOfAssurance.LEVEL_2));
        when(samlEngineProxy.translateMatchingServiceResponse(any())).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleUserAccountCreatedResponseFromMatchingService(any());
    }

    @Test
    public void handle_shouldLogToEventSinkAndNotifyPolicyOnRequesterError() {
        final InboundResponseFromMatchingServiceDto inboundResponseFromMatchingServiceDto =
                new InboundResponseFromMatchingServiceDto(MatchingServiceIdaStatus.RequesterError,
                        inResponseTo,
                        "issuer",
                        Optional.<String>absent(),
                        Optional.<LevelOfAssurance>absent());
        when(samlEngineProxy.translateMatchingServiceResponse(any())).thenReturn(inboundResponseFromMatchingServiceDto);

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleRequestFailure();
        verify(eventLogger).logErrorEvent(format("Requester error in response from matching service for session {0}", sessionId), sessionId);
    }

    @Test
    public void handle_shouldLogToEventSinkAndUpdateStateWhenHandlingError() {
        matchingServiceResponseService.handleFailure(sessionId);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleRequestFailure();
        verify(eventLogger).logErrorEvent(format("received failure notification from saml-soap-proxy for session {0}", sessionId), sessionId);
    }

    @Test
    public void handle_shouldUpdateStateWhenSamlProxyCannotProcessSaml() {
        when(samlEngineProxy.translateMatchingServiceResponse(any())).thenThrow(ApplicationException.createAuditedException(ExceptionType.INVALID_SAML, UUID.randomUUID()));

        matchingServiceResponseService.handleSuccessResponse(sessionId, samlResponseDto);

        verify(waitingForMatchingServiceResponseStateController, times(1)).handleRequestFailure();
        verify(eventLogger).logErrorEvent("Error translating matching service response", sessionId);
    }
}
