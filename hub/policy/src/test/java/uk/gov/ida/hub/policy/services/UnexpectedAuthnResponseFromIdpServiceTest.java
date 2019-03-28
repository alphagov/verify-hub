package uk.gov.ida.hub.policy.services;

import org.eclipse.jetty.server.session.SessionDataStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.builder.AttributeQueryContainerDtoBuilder;
import uk.gov.ida.hub.policy.builder.AttributeQueryRequestBuilder;
import uk.gov.ida.hub.policy.builder.SamlAuthnResponseTranslatorDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.InboundResponseFromIdpDtoBuilder;
import uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder;
import uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder;
import uk.gov.ida.hub.policy.builder.domain.SuccessFromIdpBuilder;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.domain.AuthenticationErrorResponse;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.FraudFromIdp;
import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.InboundResponseFromIdpDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.RequesterErrorResponse;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.SuccessFromIdp;
import uk.gov.ida.hub.policy.domain.controller.IdpSelectedStateController;
import uk.gov.ida.hub.policy.domain.controller.SessionStartedStateController;
import uk.gov.ida.hub.policy.domain.controller.StateControllerFactory;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateException;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.session.SessionStore;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;
import static uk.gov.ida.hub.policy.builder.domain.AuthenticationErrorResponseBuilder.anAuthenticationErrorResponse;
import static uk.gov.ida.hub.policy.builder.domain.RequesterErrorResponseBuilder.aRequesterErrorResponse;

@RunWith(MockitoJUnitRunner.class)
public class UnexpectedAuthnResponseFromIdpServiceTest {

    @Mock
    private SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory;
    @Mock
    private SamlEngineProxy samlEngineProxy;
    @Mock
    private IdpSelectedStateController idpSelectedStateController;
    @Mock
    private SessionStartedStateController sessionStartedStateController;
    @Mock
    private SessionStore sessionStore;
    @Mock
    private AttributeQueryService attributeQueryService;
    @Mock
    private StateControllerFactory controllerFactory;

    private AuthnResponseFromIdpService service;
    private SamlAuthnResponseContainerDto samlAuthnResponseContainerDto;
    private SessionId sessionId;
    private static final String PRINCIPAL_IP_ADDRESS = "1.1.1.1";
    private static final boolean REGISTERING = true;
    private static final String ANALYTICS_SESSION_ID = "some-analytics-session-id";
    private static final String JOURNEY_TYPE = "some-journey-type";
    private static final String MSA_ENTITY_ID = "a-msa-entity-id";
    private static final String REQUEST_ISSUER_ID = "request-issuer-id";

    @Before
    public void setup() {
        SessionRepository sessionRepository = new SessionRepository(sessionStore, controllerFactory);
        sessionId = SessionIdBuilder.aSessionId().build();
        SessionStartedState sessionStartedState = new SessionStartedState("testRequestId", "testRelayState", REQUEST_ISSUER_ID, null, true, null, sessionId, false);
        sessionRepository.createSession(sessionStartedState);

        when(sessionStore.get(sessionId)).thenReturn(sessionStartedState);
        when(sessionStore.hasSession(sessionId)).thenReturn(true);

        when(controllerFactory.build(any(), any())).thenReturn(sessionStartedStateController);

        samlAuthnResponseContainerDto = aSamlAuthnResponseContainerDto().withSessionId(sessionId).withPrincipalIPAddressAsSeenByHub(
                PRINCIPAL_IP_ADDRESS).withAnalyticsSessionId(ANALYTICS_SESSION_ID).withJourneyType(JOURNEY_TYPE).build();
        service = new AuthnResponseFromIdpService(samlEngineProxy, attributeQueryService, sessionRepository, samlAuthnResponseTranslatorDtoFactory);
    }

    @Test(expected= InvalidSessionStateException.class) // Replace this with a unique exception for our error
    public void shouldHandleInvalidState() {
        // Given
        InboundResponseFromIdpDto responseFromIdp = InboundResponseFromIdpDtoBuilder.noAuthnContextResponse("testIdpEntityId");
        when(samlEngineProxy.translateAuthnResponseFromIdp(any())).thenReturn(responseFromIdp);

        // When
        service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);
    }
}
