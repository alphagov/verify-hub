package uk.gov.ida.hub.policy.controllogic;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.SamlResponseWithAuthnRequestInformationDto;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.IdpSelected;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.StateController;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectingStateController;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.SamlResponseWithAuthnRequestInformationDtoBuilder;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthnRequestFromTransactionHandlerTest {
    private static final String IDP_ENTITY_ID = "anIdpEntityId";
    private static final String PRINCIPAL_IP_ADDRESS = "aPrincipalIpAddress";
    private static final boolean REGISTERING = true;

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    private AuthnRequestFromTransactionHandler authnRequestFromTransactionHandler;

    @Before
    public void setUp() {
        authnRequestFromTransactionHandler = new AuthnRequestFromTransactionHandler(sessionRepository, eventSinkHubEventLogger, policyConfiguration, transactionsConfigProxy);
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

        verify(eventSinkHubEventLogger, times(1)).logSessionStartedEvent(Matchers.<SamlResponseWithAuthnRequestInformationDto>any(), anyString(), Matchers.<DateTime>any(), Matchers.<SessionId>any(), Matchers.<LevelOfAssurance>any(), Matchers.<LevelOfAssurance>any());

    }

    @Test
    public void stateControllerInvokedFromSessionRepositoryForselectedIdp() {
        SessionId sessionId = new SessionId("aSessionId");
        IdpSelected idpSelected = new IdpSelected(IDP_ENTITY_ID, PRINCIPAL_IP_ADDRESS, REGISTERING);

        IdpSelectingStateControllerSpy idpSelectingStateController = new IdpSelectingStateControllerSpy();
        when(sessionRepository.getStateController(sessionId, IdpSelectingState.class)).thenReturn((idpSelectingStateController));

        authnRequestFromTransactionHandler.selectIdpForGivenSessionId(sessionId, idpSelected);

        assertThat(idpSelectingStateController.idpEntityId()).isEqualTo(IDP_ENTITY_ID);
        assertThat(idpSelectingStateController.principalIpAddress()).isEqualTo(PRINCIPAL_IP_ADDRESS);
        assertThat(idpSelectingStateController.registering()).isEqualTo(REGISTERING);
    }

    private class IdpSelectingStateControllerSpy implements IdpSelectingStateController, StateController {
        private String idpEntityId = null;
        private String principalIpAddress = null;
        private boolean registering = false;

        @Override
        public void handleIdpSelected(String idpEntityId, String principalIpAddress, boolean registering) {
            this.idpEntityId= idpEntityId;
            this.principalIpAddress = principalIpAddress;
            this.registering = registering;
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
    }
}
