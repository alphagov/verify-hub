package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.CountryAuthenticationStatus;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.TransactionIdaStatus;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.EidasAttributeQueryRequestDtoBuilder.anEidasAttributeQueryRequestDto;
import static uk.gov.ida.hub.policy.builder.state.EidasAuthnFailedErrorStateBuilder.anEidasAuthnFailedErrorState;
import static uk.gov.ida.hub.policy.builder.state.EidasCountrySelectedStateBuilder.anEidasCountrySelectedState;
import static uk.gov.ida.hub.policy.domain.LevelOfAssurance.LEVEL_1;
import static uk.gov.ida.hub.policy.domain.LevelOfAssurance.LEVEL_2;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_COUNTRY_ONE;
import static uk.gov.ida.saml.core.test.TestEntityIds.STUB_IDP_ONE;

@RunWith(MockitoJUnitRunner.class)
public class EidasCountrySelectedStateControllerTest {

    private static final String MSA_ID = "msa-id";
    private static final DateTime NOW = DateTime.now(DateTimeZone.UTC);
    private static final String PID = "pid";
    private static final String BLOB = "blob";
    private static final String IP_ADDRESS = "ip-address";
    private static final String SELECTED_COUNTRY = STUB_COUNTRY_ONE;
    private static final String ANALYTICS_SESSION_ID = "some-session-id";
    private static final String JOURNEY_TYPE = "some-journey-type";

    private static final InboundResponseFromCountry INBOUND_RESPONSE_FROM_COUNTRY  = new InboundResponseFromCountry(
            CountryAuthenticationStatus.Status.Success,
            Optional.absent(),
            STUB_COUNTRY_ONE,
            Optional.of(BLOB),
            Optional.of(PID),
            Optional.of(LEVEL_2),
            Optional.absent());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private HubEventLogger hubEventLogger;

    @Mock
    private StateTransitionAction stateTransitionAction;

    @Mock
    private AssertionRestrictionsFactory assertionRestrictionsFactory;

    @Mock
    private PolicyConfiguration policyConfiguration;

    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;

    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    private EidasCountrySelectedState state = anEidasCountrySelectedState()
            .withSelectedCountry(SELECTED_COUNTRY)
            .withLevelOfAssurance(ImmutableList.of(LevelOfAssurance.LEVEL_2))
            .withTransactionSupportsEidas(true)
            .withForceAuthentication(false)
            .build();

    private EidasCountrySelectedStateController controller;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        controller = new EidasCountrySelectedStateController(
                state,
                hubEventLogger,
                stateTransitionAction,
                policyConfiguration,
                transactionsConfigProxy,
                matchingServiceConfigProxy,
                new ResponseFromHubFactory(new IdGenerator()),
                assertionRestrictionsFactory
        );
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldReturnMatchingServiceEntityIdWhenAsked() {
        when(transactionsConfigProxy.getMatchingServiceEntityId(STUB_COUNTRY_ONE)).thenReturn(MSA_ID);
        controller.getMatchingServiceEntityId();
        verify(transactionsConfigProxy).getMatchingServiceEntityId(state.getRequestIssuerEntityId());
    }

    @Test
    public void shouldThrowIfPidNotPresentInTranslatedResponse() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage(String.format("Authn translation for request %s failed with missing mandatory attribute %s", state.getRequestId(), "persistentId"));

        final InboundResponseFromCountry inboundResponseFromCountry = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.absent(),
                STUB_COUNTRY_ONE,
                Optional.of(BLOB),
                Optional.absent(),
                Optional.of(LEVEL_2),
                Optional.absent());

        controller.handleMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);
    }

    @Test
    public void shouldThrowIfIdentityBlobNotPresentInTranslatedResponse() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage(String.format("Authn translation for request %s failed with missing mandatory attribute %s", state.getRequestId(), "encryptedIdentityAssertionBlob"));

        final InboundResponseFromCountry inboundResponseFromCountry = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.absent(),
                STUB_COUNTRY_ONE,
                Optional.absent(),
                Optional.of(PID),
                Optional.of(LEVEL_2),
                Optional.absent());

        controller.handleMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);
    }

    @Test
    public void shouldValidateAbsentLoa() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage("No level of assurance in the response.");

        final InboundResponseFromCountry inboundResponseFromCountry = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.absent(),
                STUB_COUNTRY_ONE,
                Optional.of(BLOB),
                Optional.of(PID),
                Optional.absent(),
                Optional.absent());

        controller.handleMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);
    }

    @Test
    public void shouldValidateIncorrectLoa() {
        exception.expect(StateProcessingValidationException.class);
        exception.expectMessage(
                String.format("Level of assurance in the response does not match level of assurance in the request. Was [%s] but expected [%s].",
                        LEVEL_1, ImmutableList.of(LEVEL_2)));

        final InboundResponseFromCountry inboundResponseFromCountry = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.absent(),
                STUB_IDP_ONE,
                Optional.of(BLOB),
                Optional.of(PID),
                Optional.of(LEVEL_1),
                Optional.absent());

        controller.handleMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);
    }

    @Test
    public void shouldNotThrowWhenValidatingCorrectLoa() {
        controller.handleMatchingJourneySuccessResponseFromCountry(INBOUND_RESPONSE_FROM_COUNTRY, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);
    }

    @Test
    public void shouldTransitionToEidasCycle0And1MatchRequestSentStateWhenUsingMatching() {
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn(MSA_ID);
        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = anEidasAttributeQueryRequestDto().build();

        EidasCycle0And1MatchRequestSentState eidasCycle0And1MatchRequestSentState = new EidasCycle0And1MatchRequestSentState(
            state.getRequestId(),
            state.getRequestIssuerEntityId(),
            state.getSessionExpiryTimestamp(),
            state.getAssertionConsumerServiceUri(),
            new SessionId(state.getSessionId().getSessionId()),
            state.getTransactionSupportsEidas(),
            STUB_COUNTRY_ONE,
            state.getRelayState().orNull(),
            eidasAttributeQueryRequestDto.getLevelOfAssurance(),
            MSA_ID,
            eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion(),
            eidasAttributeQueryRequestDto.getPersistentId(),
                false
        );

        InboundResponseFromCountry inboundResponseFromCountry = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.absent(),
                STUB_COUNTRY_ONE,
                Optional.of(eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion()),
                Optional.of(eidasAttributeQueryRequestDto.getPersistentId().getNameId()),
                Optional.of(LEVEL_2),
                Optional.absent());

        controller.handleMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        verify(hubEventLogger).logIdpAuthnSucceededEvent(
            state.getSessionId(),
            state.getSessionExpiryTimestamp(),
            state.getCountryEntityId(),
            state.getRequestIssuerEntityId(),
            eidasAttributeQueryRequestDto.getPersistentId(),
            state.getRequestId(),
            state.getLevelsOfAssurance().get(0),
            state.getLevelsOfAssurance().get(state.getLevelsOfAssurance().size() - 1),
            eidasAttributeQueryRequestDto.getLevelOfAssurance(),
            com.google.common.base.Optional.absent(),
            IP_ADDRESS,
            ANALYTICS_SESSION_ID,
            JOURNEY_TYPE
    );

        verify(stateTransitionAction).transitionTo(eidasCycle0And1MatchRequestSentState);
    }

    @Test
    public void shouldTransitionNonMatchingJourneySuccessStateWhenNotUsingMatching() {
        when(transactionsConfigProxy.isUsingMatching(state.getRequestIssuerEntityId())).thenReturn(false);
        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = anEidasAttributeQueryRequestDto().build();

        InboundResponseFromCountry inboundResponseFromCountry = new InboundResponseFromCountry(
                CountryAuthenticationStatus.Status.Success,
                Optional.absent(),
                STUB_COUNTRY_ONE,
                Optional.of(eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion()),
                Optional.of(eidasAttributeQueryRequestDto.getPersistentId().getNameId()),
                Optional.of(LEVEL_2),
                Optional.absent());

        controller.handleNonMatchingJourneySuccessResponseFromCountry(inboundResponseFromCountry, IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        verify(hubEventLogger).logIdpAuthnSucceededEvent(
                state.getSessionId(),
                state.getSessionExpiryTimestamp(),
                state.getCountryEntityId(),
                state.getRequestIssuerEntityId(),
                eidasAttributeQueryRequestDto.getPersistentId(),
                state.getRequestId(),
                state.getLevelsOfAssurance().get(0),
                state.getLevelsOfAssurance().get(state.getLevelsOfAssurance().size() - 1),
                eidasAttributeQueryRequestDto.getLevelOfAssurance(),
                com.google.common.base.Optional.absent(),
                IP_ADDRESS,
                ANALYTICS_SESSION_ID,
                JOURNEY_TYPE
        );

        NonMatchingJourneySuccessState nonMatchingJourneySuccessState = new NonMatchingJourneySuccessState(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                new SessionId(state.getSessionId().getSessionId()),
                state.getTransactionSupportsEidas(),
                state.getRelayState(),
                singleton(eidasAttributeQueryRequestDto.getEncryptedIdentityAssertion())
        );

        ArgumentCaptor<NonMatchingJourneySuccessState> stateArgumentCaptor = ArgumentCaptor.forClass(NonMatchingJourneySuccessState.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isEqualToComparingFieldByField(nonMatchingJourneySuccessState);
    }

    @Test
    public void shouldReturnNoAuthContextErrorResponse() {
        ResponseFromHub errorResponse = controller.getErrorResponse();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(TransactionIdaStatus.NoAuthenticationContext);
        assertThat(errorResponse.getAuthnRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(errorResponse.getInResponseTo()).isEqualTo(state.getRequestId());
        assertThat(errorResponse.getRelayState()).isEqualTo(state.getRelayState());
        assertThat(errorResponse.getAssertionConsumerServiceUri()).isEqualTo(state.getAssertionConsumerServiceUri());
    }

    @Test
    public void shouldLogAndTransitionToCountryAuthFailedErrorStateOnAuthnFailedResponseFromCountry() {
        ArgumentCaptor<EidasAuthnFailedErrorState> capturedState = ArgumentCaptor.forClass(EidasAuthnFailedErrorState.class);

        EidasAuthnFailedErrorState expectedState = anEidasAuthnFailedErrorState()
                .withSessionExpiryTimestamp(state.getSessionExpiryTimestamp())
                .withSessionId(state.getSessionId())
                .withRequestId(state.getRequestId())
                .withRequestIssuerId(state.getRequestIssuerEntityId())
                .withCountryEntityId(SELECTED_COUNTRY)
                .withForceAuthentication(false)
                .build();

        controller.handleAuthenticationFailedResponseFromCountry(IP_ADDRESS, ANALYTICS_SESSION_ID, JOURNEY_TYPE);

        verify(hubEventLogger).logIdpAuthnFailedEvent(
                state.getSessionId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestId(),
                IP_ADDRESS,
                ANALYTICS_SESSION_ID,
                JOURNEY_TYPE);

        verify(stateTransitionAction).transitionTo(capturedState.capture());
        assertThat(capturedState.getValue()).isInstanceOf(EidasAuthnFailedErrorState.class);
        assertThat(capturedState.getValue()).isEqualToComparingFieldByField(expectedState);
    }

    @Test
    public void shouldTransitionToSessionStartedStateAndLogEvent() {
        controller.transitionToSessionStartedState();
        ArgumentCaptor<SessionStartedState> capturedState = ArgumentCaptor.forClass(SessionStartedState.class);

        verify(stateTransitionAction, times(1)).transitionTo(capturedState.capture());
        verify(hubEventLogger, times(1)).logSessionMovedToStartStateEvent(capturedState.getValue());

        assertThat(capturedState.getValue().getSessionId()).isEqualTo(state.getSessionId());
        assertThat(capturedState.getValue().getRequestIssuerEntityId()).isEqualTo(state.getRequestIssuerEntityId());
        assertThat(capturedState.getValue().getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(capturedState.getValue().getForceAuthentication().orNull()).isEqualTo(false);
    }
}
