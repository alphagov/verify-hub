package uk.gov.ida.hub.policy.controllogic;

import com.google.common.base.Optional;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.controller.RestartJourneyStateController;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectingStateController;
import uk.gov.ida.hub.policy.domain.state.RestartJourneyState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthnRequestFromTransactionHandlerTest {
    private static final String IDP_ENTITY_ID = "anIdpEntityId";
    private static final String PRINCIPAL_IP_ADDRESS = "aPrincipalIpAddress";
    private static final boolean REGISTERING = true;
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;
    private static final String ANALYTICS_SESSION_ID = "anAnalyticsSessionId";
    private static final String JOURNEY_TYPE = "aJourneyType";
    private static final IdGenerator idGenerator = new IdGenerator();

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private HubEventLogger hubEventLogger;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private RestartJourneyStateController restartJourneyStateController;

    private AuthnRequestFromTransactionHandler authnRequestFromTransactionHandler;

    @Before
    public void setUp() {
        authnRequestFromTransactionHandler = new AuthnRequestFromTransactionHandler(sessionRepository, hubEventLogger, policyConfiguration, transactionsConfigProxy, idGenerator);
    }

    @Test
    public void testHandleRequestFromTransaction_logsToEventSink() {
        final SamlResponseWithAuthnRequestInformationDto samlResponseWithAuthnRequestInformationDto = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().build();
        final String ipAddress = "ipaddress";
        final URI assertionConsumerServiceUri = URI.create("blah");
        final Optional<String> relayState = Optional.of("relaystate");

        when(policyConfiguration.getSessionLength()).thenReturn(Duration.standardHours(1));
        when(transactionsConfigProxy.getLevelsOfAssurance(samlResponseWithAuthnRequestInformationDto.getIssuer())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_1));

        authnRequestFromTransactionHandler.handleRequestFromTransaction(samlResponseWithAuthnRequestInformationDto, relayState, ipAddress, assertionConsumerServiceUri, false);

        verify(hubEventLogger, times(1)).logSessionStartedEvent(any(), anyString(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    }

    @Test
    public void stateControllerInvokedFromSessionRepositoryForselectedIdp() {
        SessionId sessionId = new SessionId("aSessionId");
        IdpSelected idpSelected = new IdpSelected(IDP_ENTITY_ID, PRINCIPAL_IP_ADDRESS, REGISTERING, REQUESTED_LOA, ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        IdpSelectingStateControllerSpy idpSelectingStateController = new IdpSelectingStateControllerSpy();
        when(sessionRepository.getStateController(sessionId, IdpSelectingState.class)).thenReturn((idpSelectingStateController));

        authnRequestFromTransactionHandler.selectIdpForGivenSessionId(sessionId, idpSelected);

        assertThat(idpSelectingStateController.idpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectingStateController.principalIpAddress()).isEqualTo(PRINCIPAL_IP_ADDRESS);
        assertThat(idpSelectingStateController.registering()).isEqualTo(REGISTERING);
        assertThat(idpSelectingStateController.getRequestedLoa()).isEqualTo(REQUESTED_LOA);
    }

    @Test
    public void restartsJourney() {
        SessionId sessionId = new SessionId("sessionId");
        when(sessionRepository.getStateController(sessionId, RestartJourneyState.class)).thenReturn(restartJourneyStateController);

        authnRequestFromTransactionHandler.restartJourney(sessionId);

        verify(restartJourneyStateController).transitionToSessionStartedState();
    }

    private static class IdpSelectingStateControllerSpy implements IdpSelectingStateController, StateController {
        private String idpEntityId = null;
        private String principalIpAddress = null;
        private boolean registering = false;
        private LevelOfAssurance requestedLoa = null;
        private String analyticsSessionId = null;
        private String journeyType = null;


        @Override
        public void handleIdpSelected(String idpEntityId, String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa, String analyticsSessionId, String journeyType) {
            this.idpEntityId= idpEntityId;
            this.principalIpAddress = principalIpAddress;
            this.registering = registering;
            this.requestedLoa = requestedLoa;
            this.analyticsSessionId = analyticsSessionId;
            this.journeyType = journeyType;
        }

        @Override
        public String getRequestIssuerId() {
            return null;
        }

        @Override
        public AuthnRequestSignInProcess getSignInProcessDetails() {
            return null;
        }

        String idpEntityId() {
            return this.idpEntityId;
        }

        String principalIpAddress() {
            return this.principalIpAddress;
        }

        boolean registering() {
            return this.registering;
        }

        public LevelOfAssurance getRequestedLoa() {
            return requestedLoa;
        }
    }
}
