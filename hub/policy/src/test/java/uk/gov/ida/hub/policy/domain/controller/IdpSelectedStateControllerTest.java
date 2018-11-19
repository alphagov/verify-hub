package uk.gov.ida.hub.policy.domain.controller;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.AuthenticationErrorResponse;
import uk.gov.ida.hub.policy.domain.AuthnRequestSignInProcess;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.FraudFromIdp;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.RequesterErrorResponse;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.StateTransitionAction;
import uk.gov.ida.hub.policy.domain.SuccessFromIdp;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingJourneySuccessState;
import uk.gov.ida.hub.policy.domain.state.PausedRegistrationState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.logging.HubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.MatchingServiceConfigEntityDataDtoBuilder.aMatchingServiceConfigEntityDataDto;
import static uk.gov.ida.hub.policy.builder.domain.AuthenticationErrorResponseBuilder.anAuthenticationErrorResponse;
import static uk.gov.ida.hub.policy.builder.domain.FraudFromIdpBuilder.aFraudFromIdp;
import static uk.gov.ida.hub.policy.builder.domain.IdpConfigDtoBuilder.anIdpConfigDto;
import static uk.gov.ida.hub.policy.builder.domain.PersistentIdBuilder.aPersistentId;
import static uk.gov.ida.hub.policy.builder.domain.RequesterErrorResponseBuilder.aRequesterErrorResponse;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;
import static uk.gov.ida.hub.policy.builder.domain.SuccessFromIdpBuilder.aSuccessFromIdp;
import static uk.gov.ida.hub.policy.builder.state.IdpSelectedStateBuilder.anIdpSelectedState;

@RunWith(MockitoJUnitRunner.class)
public class IdpSelectedStateControllerTest {

    private static final String IDP_ENTITY_ID = "some-idp-issuer-id";
    private static final String TRANSACTION_ENTITY_ID = "transaction-entity-id";
    private static final SessionId NEW_SESSION_ID = aSessionId().build();
    private static final List<LevelOfAssurance> LEVELS_OF_ASSURANCE = asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
    private static final LevelOfAssurance PROVIDED_LOA = LevelOfAssurance.LEVEL_2;
    private static final String PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP = "principal-ip-address-from-idp";
    private static final String PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB = "principal-ip-address-from-hub";
    private static final DateTime SESSION_EXPIRY_TIMESTAMP = DateTime.now().plusMinutes(15);
    private static final String REQUEST_ID = UUID.randomUUID().toString();
    private static final URI ATTRIBUTE_QUERY_URI = URI.create("/attribute-query-uri");

    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private HubEventLogger hubEventLogger;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private AssertionRestrictionsFactory assertionRestrictionsFactory;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;

    private IdpSelectedStateController controller;
    private IdpSelectedState idpSelectedState;

    @Before
    public void setup() {
        controller = idpSelectedStateBuilder(false);
    }

    private IdpSelectedStateController idpSelectedStateBuilder(boolean isRegistration) {
        idpSelectedState = anIdpSelectedState()
                .withSessionId(NEW_SESSION_ID)
                .withIdpEntityId(IDP_ENTITY_ID)
                .withRequestId(REQUEST_ID)
                .withLevelsOfAssurance(LEVELS_OF_ASSURANCE)
                .withSessionExpiryTimestamp(SESSION_EXPIRY_TIMESTAMP)
                .withRegistration(isRegistration)
                .withTransactionSupportsEidas(true)
                .build();
        IdpSelectedState state = idpSelectedState;

        String matchingServiceEntityId = "matching-service-entity-id";
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn(matchingServiceEntityId);
        when(matchingServiceConfigProxy.getMatchingService(matchingServiceEntityId)).thenReturn(aMatchingServiceConfigEntityDataDto().withUri(ATTRIBUTE_QUERY_URI).build());

        return new IdpSelectedStateController(
                state,
                hubEventLogger,
                stateTransitionAction,
                identityProvidersConfigProxy,
                transactionsConfigProxy,
                responseFromHubFactory,
                policyConfiguration,
                assertionRestrictionsFactory,
                matchingServiceConfigProxy
        );
    }

    @Test
    public void getSignInProcessDetails_shouldReturnFieldsFromTheState() {
        AuthnRequestSignInProcess signInProcessDetails = controller.getSignInProcessDetails();
        assertThat(signInProcessDetails.getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(signInProcessDetails.getRequestIssuerId()).isEqualTo("transaction-entity-id");
    }

    @Test
    public void handleResponseFromIdp_shouldTransitionToAuthnFailedStateWhenFraudHasOccurred() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        FraudFromIdp fraudFromIdp = aFraudFromIdp()
                .withIssuerId(IDP_ENTITY_ID)
                .withFraudDetectedDetails(new FraudDetectedDetails("id", "IT01"))
                .build();

        controller.handleFraudResponseFromIdp(fraudFromIdp);

        ArgumentCaptor<State> stateArgumentCaptor = ArgumentCaptor.forClass(State.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(FraudEventDetectedState.class);
    }

    @Test
    public void handleResponseFromIfp_whenFraudHasOccurred_shouldSendFraudHubEvent() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        FraudDetectedDetails idpFraudDetectedDetails = new FraudDetectedDetails("id", "IT01");
        FraudFromIdp fraudFromIdp = aFraudFromIdp()
                .withIssuerId(IDP_ENTITY_ID)
                .withFraudDetectedDetails(idpFraudDetectedDetails)
                .withPrincipalIpAddressSeenByIdp(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .build();

        controller.handleFraudResponseFromIdp(fraudFromIdp);

        verify(hubEventLogger).logIdpFraudEvent(NEW_SESSION_ID, IDP_ENTITY_ID, TRANSACTION_ENTITY_ID, fraudFromIdp.getPersistentId(), SESSION_EXPIRY_TIMESTAMP, idpFraudDetectedDetails, Optional.fromNullable(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP), PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB, REQUEST_ID);
    }

    @Test
    public void handleResponseFromIdp_shouldTransitionToAuthnFailedStateWhenAGenericAuthenticationFailureHasOccurred() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(IDP_ENTITY_ID)
                .build();

        controller.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);

        ArgumentCaptor<State> stateArgumentCaptor = ArgumentCaptor.forClass(State.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(AuthnFailedErrorState.class);
    }

    @Test
    public void handleResponseFromIdp_shouldTransitionToAuthnPendingStateWhenSaveAndContinue() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));

        controller.handlePausedRegistrationResponseFromIdp(IDP_ENTITY_ID, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB, java.util.Optional.of(PROVIDED_LOA));

        ArgumentCaptor<State> stateArgumentCaptor = ArgumentCaptor.forClass(State.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(PausedRegistrationState.class);
    }

    @Test
    public void shouldTransitionToSessionStartedStateAndLogEvent() {
        controller.transitionToSessionStartedState();
        ArgumentCaptor<SessionStartedState> capturedState = ArgumentCaptor.forClass(SessionStartedState.class);

        verify(stateTransitionAction, times(1)).transitionTo(capturedState.capture());
        verify(hubEventLogger, times(1)).logSessionMovedToStartStateEvent(capturedState.getValue());

        assertThat(capturedState.getValue().getSessionId()).isEqualTo(idpSelectedState.getSessionId());
        assertThat(capturedState.getValue().getRequestIssuerEntityId()).isEqualTo(idpSelectedState.getRequestIssuerEntityId());
        assertThat(capturedState.getValue().getForceAuthentication()).isEqualTo(idpSelectedState.getForceAuthentication());
    }

    @Test(expected = IdpDisabledException.class)
    public void handleSuccessResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() {
        SuccessFromIdp successFromIdp = aSuccessFromIdp().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(emptyList());
        controller.handleMatchingJourneySuccessResponseFromIdp(successFromIdp);
    }

    @Test(expected = StateProcessingValidationException.class)
    public void handleSuccessResponseFromIdp_shouldThrowExceptionWhenReturnedLOAIsUnsupportedByIdpConfig() {
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId(IDP_ENTITY_ID)
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .withLevelOfAssurance(LevelOfAssurance.LEVEL_3)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(LEVELS_OF_ASSURANCE).build());

        controller.handleMatchingJourneySuccessResponseFromIdp(successFromIdp);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleAuthenticationFailedResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(emptyList());
        controller.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);
    }

    @Test
    public void handleNoAuthenticationContextResponseFromIdp_shouldTransitionToAuthnFailedErrorStateWhenRegistrationCancelled() {
        controller = idpSelectedStateBuilder(true);

        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().withIssuerId(IDP_ENTITY_ID).build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(any(String.class), eq(controller.isRegistrationContext()), eq(PROVIDED_LOA)))
                .thenReturn(singletonList(authenticationErrorResponse.getIssuer()));
        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);
        verify(stateTransitionAction).transitionTo(isA(AuthnFailedErrorState.class));
    }

    @Test
    public void handleNoAuthenticationContextResponseFromIdp_shouldTransitionToSessionCreatedStateWhenSigninCancelled() {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().withIssuerId(IDP_ENTITY_ID).build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(any(String.class), eq(controller.isRegistrationContext()), eq(PROVIDED_LOA)))
                .thenReturn(singletonList(authenticationErrorResponse.getIssuer()));
        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);
        verify(stateTransitionAction).transitionTo(isA(SessionStartedState.class));
    }

    @Test(expected = IdpDisabledException.class)
    public void handleNoAuthenticationContextResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(emptyList());
        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleFraudResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() {
        FraudFromIdp fraudFromIdp = aFraudFromIdp().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(emptyList());
        controller.handleFraudResponseFromIdp(fraudFromIdp);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleRequesterErrorResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() {
        RequesterErrorResponse requesterErrorResponse = aRequesterErrorResponse().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(emptyList());
        controller.handleRequesterErrorResponseFromIdp(requesterErrorResponse);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleRequesterPendingResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(emptyList());
        controller.handlePausedRegistrationResponseFromIdp(IDP_ENTITY_ID, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB, java.util.Optional.of(PROVIDED_LOA));
    }

    @Test
    public void handleMatchingJourneySuccessResponseFromIdp_shouldTransitionToCycle0And1MatchRequestSentState() {
        ArgumentCaptor<Cycle0And1MatchRequestSentState> stateArgumentCaptor = ArgumentCaptor.forClass(Cycle0And1MatchRequestSentState.class);
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        final String encryptedMatchingDatasetAssertion = "blah";
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId(IDP_ENTITY_ID)
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .withLevelOfAssurance(PROVIDED_LOA)
                .withEncryptedMatchingDatasetAssertion(encryptedMatchingDatasetAssertion)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(LEVELS_OF_ASSURANCE).build());

        controller.handleMatchingJourneySuccessResponseFromIdp(successFromIdp);

        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(Cycle0And1MatchRequestSentState.class);
        assertThat(stateArgumentCaptor.getValue().getEncryptedMatchingDatasetAssertion()).isEqualTo(encryptedMatchingDatasetAssertion);
    }

    @Test
    public void handleNonMatchingJourneySuccessResponseFromIdp_shouldTransitionToNonMatchingJourneySuccessState() {
        ArgumentCaptor<NonMatchingJourneySuccessState> stateArgumentCaptor = ArgumentCaptor.forClass(NonMatchingJourneySuccessState.class);
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        final String encryptedMatchingDatasetAssertion = "blah";
        final String authnStatementAssertion = "blah2";
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
            .withIssuerId(IDP_ENTITY_ID)
            .withPersistentId(persistentId)
            .withPrincipalIpAddressSeenByIdp(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP)
            .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
            .withLevelOfAssurance(PROVIDED_LOA)
            .withEncryptedMatchingDatasetAssertion(encryptedMatchingDatasetAssertion)
            .withAuthnStatementAssertion(authnStatementAssertion)
            .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
            .thenReturn(singletonList(IDP_ENTITY_ID));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(LEVELS_OF_ASSURANCE).build());

        controller.handleNonMatchingJourneySuccessResponseFromIdp(successFromIdp);

        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(NonMatchingJourneySuccessState.class);
        Set<String> expectedAssertions = new HashSet<>(Arrays.asList(encryptedMatchingDatasetAssertion, authnStatementAssertion));
        assertThat(stateArgumentCaptor.getValue().getEncryptedAssertions()).isEqualTo(expectedAssertions);
    }

    @Test
    public void handleSuccessResponseFromIdp_shouldLogEventContainingLvlOfAssuranceForBillingAndPrincipalIpAddressSeenByIdpAndHub() {
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId(IDP_ENTITY_ID)
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .withLevelOfAssurance(PROVIDED_LOA)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(IDP_ENTITY_ID)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(LEVELS_OF_ASSURANCE).build());
        controller.handleMatchingJourneySuccessResponseFromIdp(successFromIdp);
        verify(hubEventLogger).logIdpAuthnSucceededEvent(
                NEW_SESSION_ID,
                SESSION_EXPIRY_TIMESTAMP,
                IDP_ENTITY_ID,
                TRANSACTION_ENTITY_ID,
                persistentId,
                REQUEST_ID,
                LEVELS_OF_ASSURANCE.get(0),
                LEVELS_OF_ASSURANCE.get(1),
                PROVIDED_LOA,
                Optional.fromNullable(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP),
                PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);
    }

    @Test
    public void handleRequesterPendingResponseFromIdp_shouldLogEvent() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));

        controller.handlePausedRegistrationResponseFromIdp(IDP_ENTITY_ID, PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB, java.util.Optional.of(PROVIDED_LOA));

        verify(hubEventLogger).logPausedRegistrationEvent(
                NEW_SESSION_ID,
                TRANSACTION_ENTITY_ID,
                SESSION_EXPIRY_TIMESTAMP,
                REQUEST_ID,
                PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);
    }

    @Test
    public void handleRequesterErrorResponseFromIdp_shouldLogEvent() {
        final String errorMessage = "an-error-message";
        RequesterErrorResponse requesterErrorResponse = aRequesterErrorResponse()
                .withIssuerId(IDP_ENTITY_ID)
                .withErrorMessage(errorMessage)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));

        controller.handleRequesterErrorResponseFromIdp(requesterErrorResponse);

        verify(hubEventLogger).logIdpRequesterErrorEvent(
                NEW_SESSION_ID,
                TRANSACTION_ENTITY_ID,
                SESSION_EXPIRY_TIMESTAMP,
                REQUEST_ID,
                Optional.fromNullable(errorMessage),
                PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);
    }

    @Test
    public void handleAuthnFailedResponseFromIdp_shouldLogEvent() {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(IDP_ENTITY_ID)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));

        controller.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);

        verify(hubEventLogger).logIdpAuthnFailedEvent(
                NEW_SESSION_ID,
                TRANSACTION_ENTITY_ID,
                SESSION_EXPIRY_TIMESTAMP,
                REQUEST_ID,
                PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);
    }

    @Test
    public void handleNoAuthnContextResponseFromIdp_shouldLogEvent() {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(IDP_ENTITY_ID)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(singletonList(IDP_ENTITY_ID));

        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);

        verify(hubEventLogger).logNoAuthnContextEvent(
                NEW_SESSION_ID,
                TRANSACTION_ENTITY_ID,
                SESSION_EXPIRY_TIMESTAMP,
                REQUEST_ID,
                PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB);
    }

    @Test
    public void shouldReturnMatchingServiceEntityIdWhenAsked() {
       controller.getMatchingServiceEntityId();
        verify(transactionsConfigProxy).getMatchingServiceEntityId(idpSelectedState.getRequestIssuerEntityId());
    }

    @Test(expected = StateProcessingValidationException.class)
    public void shouldThrowUnauditedErrorExceptionIfTheResponseIsFromADifferentIssuer(){
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId("differentIDP")
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_IDP)
                .withPrincipalIpAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS_AS_SEEN_BY_HUB)
                .withLevelOfAssurance(PROVIDED_LOA)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(TRANSACTION_ENTITY_ID, controller.isRegistrationContext(), PROVIDED_LOA))
                .thenReturn(asList(IDP_ENTITY_ID, "differentIDP"));

        controller.handleMatchingJourneySuccessResponseFromIdp(successFromIdp);
    }
}
