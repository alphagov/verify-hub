package uk.gov.ida.hub.policy.controllogic;

import org.joda.time.DateTime;
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
import uk.gov.ida.hub.policy.domain.controller.IdpSelectingStateController;
import uk.gov.ida.hub.policy.domain.controller.RestartJourneyStateController;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.RestartJourneyState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthnRequestFromTransactionHandlerTest {
    private static final String ANALYTICS_SESSION_ID = "anAnalyticsSessionId";
    private static final URI ASSERTION_CONSUMER_SERVICE_URI = URI.create("https://assertionConsumerServiceUri");
    private static final String COUNTRY_ENTITY_ID = "aCountryEntityId";
    private static final String ENCRYPTED_KEY = "base64EncryptedKey";
    private static final String GENERATED_ID = "generatedId";
    private static final String IDP_ENTITY_ID = "anIdpEntityId";
    private static final String JOURNEY_TYPE = "aJourneyType";
    private static final String PRINCIPAL_IP_ADDRESS = "aPrincipalIpAddress";
    private static final boolean REGISTERING = true;
    private static final String RELAY_STATE = "relayState";
    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_ISSUER_ENTITY_ID = "requestIssuerEntityId";
    private static final LevelOfAssurance REQUESTED_LOA = LevelOfAssurance.LEVEL_2;
    private static final String SAML_RESPONSE = "base64SamlResponse";
    private static final SessionId SESSION_ID = new SessionId("aSessionId");

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
    @Mock
    private IdGenerator idGenerator;

    private AuthnRequestFromTransactionHandler authnRequestFromTransactionHandler;

    @Before
    public void setUp() {
        authnRequestFromTransactionHandler = new AuthnRequestFromTransactionHandler(sessionRepository, hubEventLogger, policyConfiguration, transactionsConfigProxy, idGenerator);
    }

    @Test
    public void testHandleRequestFromTransaction_logsToEventSink() {
        final SamlResponseWithAuthnRequestInformationDto samlResponseWithAuthnRequestInformationDto = SamlResponseWithAuthnRequestInformationDtoBuilder.aSamlResponseWithAuthnRequestInformationDto().build();
        final Optional<String> relayState = Optional.of(RELAY_STATE);

        when(policyConfiguration.getSessionLength()).thenReturn(Duration.standardHours(1));
        when(transactionsConfigProxy.getLevelsOfAssurance(samlResponseWithAuthnRequestInformationDto.getIssuer())).thenReturn(asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_1));

        authnRequestFromTransactionHandler.handleRequestFromTransaction(samlResponseWithAuthnRequestInformationDto, relayState, PRINCIPAL_IP_ADDRESS, ASSERTION_CONSUMER_SERVICE_URI);

        verify(hubEventLogger, times(1)).logSessionStartedEvent(
            any(),
            anyString(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any());
    }

    @Test
    public void stateControllerInvokedFromSessionRepositoryForselectedIdp() {
        IdpSelected idpSelected = new IdpSelected(IDP_ENTITY_ID, PRINCIPAL_IP_ADDRESS, REGISTERING, REQUESTED_LOA, ANALYTICS_SESSION_ID, JOURNEY_TYPE, "");


        IdpSelectingStateControllerSpy idpSelectingStateController = new IdpSelectingStateControllerSpy();
        when(sessionRepository.getStateController(SESSION_ID, IdpSelectingState.class)).thenReturn((idpSelectingStateController));

        authnRequestFromTransactionHandler.selectIdpForGivenSessionId(SESSION_ID, idpSelected);

        assertThat(idpSelectingStateController.idpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectingStateController.principalIpAddress()).isEqualTo(PRINCIPAL_IP_ADDRESS);
        assertThat(idpSelectingStateController.registering()).isEqualTo(REGISTERING);
        assertThat(idpSelectingStateController.getRequestedLoa()).isEqualTo(REQUESTED_LOA);
    }

    @Test
    public void restartsJourney() {
        when(sessionRepository.getStateController(SESSION_ID, RestartJourneyState.class)).thenReturn(restartJourneyStateController);

        authnRequestFromTransactionHandler.restartJourney(SESSION_ID);

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
        public void handleIdpSelected(String idpEntityId, String principalIpAddress, boolean registering, LevelOfAssurance requestedLoa, String analyticsSessionId, String journeyType, String abTest) {
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
