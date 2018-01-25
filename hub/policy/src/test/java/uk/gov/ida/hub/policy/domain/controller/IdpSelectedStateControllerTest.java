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
import uk.gov.ida.hub.policy.domain.state.PausedRegistrationState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateFactory;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.logging.EventSinkHubEventLogger;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Optional.fromNullable;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
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

    private static final String idpEntityId = "some-idp-issuer-id";
    private static final String transactionEntityId = "transaction-entity-id";
    private static final SessionId NEW_SESSION_ID = aSessionId().build();
    private static final List<LevelOfAssurance> levelsOfAssurance = asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2);
    private static final LevelOfAssurance providedLevelOfAssurance = LevelOfAssurance.LEVEL_2;
    private static final Optional<String> principalIpAddressAsSeenByIdp = Optional.fromNullable("principal-ip-address-from-idp");
    private static final String principalIpAddressAsSeenByHub = "principal-ip-address-from-hub";
    private static final DateTime sessionExpiryTimestamp = DateTime.now().plusMinutes(15);
    public static final String REQUEST_ID = UUID.randomUUID().toString();
    private static final URI ATTRIBUTE_QUERY_URI = URI.create("/attribute-query-uri");

    @Mock
    private StateTransitionAction stateTransitionAction;
    @Mock
    private IdentityProvidersConfigProxy identityProvidersConfigProxy;
    @Mock
    private TransactionsConfigProxy transactionsConfigProxy;
    @Mock
    private EventSinkHubEventLogger eventSinkHubEventLogger;
    @Mock
    private ResponseFromHubFactory responseFromHubFactory;
    @Mock
    private PolicyConfiguration policyConfiguration;
    @Mock
    private AssertionRestrictionsFactory assertionRestrictionsFactory;

    private IdpSelectedStateController controller;
    @Mock
    private MatchingServiceConfigProxy matchingServiceConfigProxy;
    @Mock
    private SamlEngineProxy samlEngineProxy;

    private SessionStartedStateFactory sessionStartedStateFactory;
    private IdpSelectedState idpSelectedState;

    @Before
    public void setup() {
        sessionStartedStateFactory = new SessionStartedStateFactory(identityProvidersConfigProxy);
        controller = idpSelectedStateBuilder(false);
    }

    private IdpSelectedStateController idpSelectedStateBuilder(boolean isRegistration) {
        idpSelectedState = anIdpSelectedState()
                .withSessionId(NEW_SESSION_ID)
                .withIdpEntityId(idpEntityId)
                .withRequestId(REQUEST_ID)
                .withLevelsOfAssurance(levelsOfAssurance)
                .withSessionExpiryTimestamp(sessionExpiryTimestamp)
                .withRegistration(isRegistration)
                .withTransactionSupportsEidas(true)
                .build();
        IdpSelectedState state = idpSelectedState;

        String matchingServiceEntityId = "matching-service-entity-id";
        when(transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId())).thenReturn(matchingServiceEntityId);
        when(matchingServiceConfigProxy.getMatchingService(matchingServiceEntityId)).thenReturn(aMatchingServiceConfigEntityDataDto().withUri(ATTRIBUTE_QUERY_URI).build());

        return new IdpSelectedStateController(
                state,
                sessionStartedStateFactory,
                eventSinkHubEventLogger,
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
    public void getSignInProcessDetails_shouldReturnFieldsFromTheState() throws Exception {
        AuthnRequestSignInProcess signInProcessDetails = controller.getSignInProcessDetails();
        assertThat(signInProcessDetails.getTransactionSupportsEidas()).isEqualTo(true);
        assertThat(signInProcessDetails.getAvailableIdentityProviderEntityIds()).containsSequence("idp-a", "idp-b", "idp-c");
        assertThat(signInProcessDetails.getRequestIssuerId()).isEqualTo("transaction-entity-id");
    }

    @Test
    public void handleResponseFromIdp_shouldTransitionToAuthnFailedStateWhenFraudHasOccurred() throws Exception {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));
        FraudFromIdp fraudFromIdp = aFraudFromIdp()
                .withIssuerId(idpEntityId)
                .withFraudDetectedDetails(new FraudDetectedDetails("id", "IT01"))
                .build();

        controller.handleFraudResponseFromIdp(fraudFromIdp);

        ArgumentCaptor<State> stateArgumentCaptor = ArgumentCaptor.forClass(State.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(FraudEventDetectedState.class);
    }

    @Test
    public void handleResponseFromIfp_whenFraudHasOccurred_shouldSendFraudHubEvent() {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));
        FraudDetectedDetails idpFraudDetectedDetails = new FraudDetectedDetails("id", "IT01");
        FraudFromIdp fraudFromIdp = aFraudFromIdp()
                .withIssuerId(idpEntityId)
                .withFraudDetectedDetails(idpFraudDetectedDetails)
                .withPrincipalIpAddressSeenByIdp(principalIpAddressAsSeenByIdp.get())
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .build();

        controller.handleFraudResponseFromIdp(fraudFromIdp);

        verify(eventSinkHubEventLogger).logIdpFraudEvent(NEW_SESSION_ID, idpEntityId, transactionEntityId, fraudFromIdp.getPersistentId(), sessionExpiryTimestamp, idpFraudDetectedDetails, principalIpAddressAsSeenByIdp, principalIpAddressAsSeenByHub, REQUEST_ID);
    }

    @Test
    public void handleResponseFromIdp_shouldTransitionToAuthnFailedStateWhenAGenericAuthenticationFailureHasOccurred() throws Exception {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(idpEntityId)
                .build();

        controller.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);

        ArgumentCaptor<State> stateArgumentCaptor = ArgumentCaptor.forClass(State.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(AuthnFailedErrorState.class);
    }

    @Test
    public void handleResponseFromIdp_shouldTransitionToAuthnPendingStateWhenSaveAndContinue() throws Exception {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));

        controller.handlePausedRegistrationResponseFromIdp(idpEntityId, principalIpAddressAsSeenByHub);

        ArgumentCaptor<State> stateArgumentCaptor = ArgumentCaptor.forClass(State.class);
        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(PausedRegistrationState.class);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleSuccessResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() throws Exception {
        SuccessFromIdp successFromIdp = aSuccessFromIdp().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(Arrays.<String>asList());
        controller.handleSuccessResponseFromIdp(successFromIdp);
    }

    @Test(expected = StateProcessingValidationException.class)
    public void handleSuccessResponseFromIdp_shouldThrowExceptionWhenReturnedLOAIsUnsupportedByIdpConfig() throws Exception {
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId(idpEntityId)
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(principalIpAddressAsSeenByIdp.get())
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .withLevelOfAssurance(LevelOfAssurance.LEVEL_3)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(idpEntityId)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(levelsOfAssurance).build());

        controller.handleSuccessResponseFromIdp(successFromIdp);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleAuthenticationFailedResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() throws Exception {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(Arrays.<String>asList());
        controller.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);
    }

    @Test
    public void handleNoAuthenticationContextResponseFromIdp_shouldTransitionToAuthnFailedErrorStateWhenRegistrationCancelled() {
        controller = idpSelectedStateBuilder(true);

        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().withIssuerId(idpEntityId).build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(any(Optional.class)))
                .thenReturn(asList(authenticationErrorResponse.getIssuer()));
        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);
        verify(stateTransitionAction).transitionTo(isA(AuthnFailedErrorState.class));
    }

    @Test
    public void handleNoAuthenticationContextResponseFromIdp_shouldTransitionToSessionCreatedStateWhenSigninCancelled() {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().withIssuerId(idpEntityId).build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(any(Optional.class)))
                .thenReturn(asList(authenticationErrorResponse.getIssuer()));
        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);
        verify(stateTransitionAction).transitionTo(isA(SessionStartedState.class));
    }

    @Test(expected = IdpDisabledException.class)
    public void handleNoAuthenticationContextResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() throws Exception {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(Arrays.<String>asList());
        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleFraudResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() throws Exception {
        FraudFromIdp fraudFromIdp = aFraudFromIdp().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(Arrays.<String>asList());
        controller.handleFraudResponseFromIdp(fraudFromIdp);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleRequesterErrorResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() throws Exception {
        RequesterErrorResponse requesterErrorResponse = aRequesterErrorResponse().build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(Arrays.<String>asList());
        controller.handleRequesterErrorResponseFromIdp(requesterErrorResponse);
    }

    @Test(expected = IdpDisabledException.class)
    public void handleRequesterPendingResponseFromIdp_shouldThrowExceptionWhenIdpIsDisabled() throws Exception {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(Arrays.<String>asList());
        controller.handlePausedRegistrationResponseFromIdp(idpEntityId, principalIpAddressAsSeenByHub);
    }

    @Test
    public void handleSuccessResponseFromIdp_shouldTransitionToCycle0And1MatchRequestSentState() throws Exception {
        ArgumentCaptor<Cycle0And1MatchRequestSentState> stateArgumentCaptor = ArgumentCaptor.forClass(Cycle0And1MatchRequestSentState.class);
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        final String encryptedMatchingDatasetAssertion = "blah";
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId(idpEntityId)
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(principalIpAddressAsSeenByIdp.get())
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .withLevelOfAssurance(providedLevelOfAssurance)
                .withEncryptedMatchingDatasetAssertion(encryptedMatchingDatasetAssertion)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(idpEntityId)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(levelsOfAssurance).build());

        controller.handleSuccessResponseFromIdp(successFromIdp);

        verify(stateTransitionAction).transitionTo(stateArgumentCaptor.capture());
        assertThat(stateArgumentCaptor.getValue()).isInstanceOf(Cycle0And1MatchRequestSentState.class);
        assertThat(stateArgumentCaptor.getValue().getEncryptedMatchingDatasetAssertion()).isEqualTo(encryptedMatchingDatasetAssertion);
    }

    @Test
    public void handleSuccessResponseFromIdp_shouldLogEventContainingLvlOfAssuranceForBillingAndPrincipalIpAddressSeenByIdpAndHub() throws Exception {
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId(idpEntityId)
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(principalIpAddressAsSeenByIdp.get())
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .withLevelOfAssurance(providedLevelOfAssurance)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));
        when(policyConfiguration.getMatchingServiceResponseWaitPeriod()).thenReturn(new org.joda.time.Duration(600L));
        when(identityProvidersConfigProxy.getIdpConfig(idpEntityId)).thenReturn(anIdpConfigDto().withLevelsOfAssurance(levelsOfAssurance).build());
        controller.handleSuccessResponseFromIdp(successFromIdp);
        verify(eventSinkHubEventLogger).logIdpAuthnSucceededEvent(
                NEW_SESSION_ID,
                sessionExpiryTimestamp,
                idpEntityId,
                transactionEntityId,
                persistentId,
                REQUEST_ID,
                levelsOfAssurance.get(0),
                levelsOfAssurance.get(1),
                providedLevelOfAssurance,
                principalIpAddressAsSeenByIdp,
                principalIpAddressAsSeenByHub);
    }

    @Test
    public void handleRequesterPendingResponseFromIdp_shouldLogEvent() throws Exception {
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));

        controller.handlePausedRegistrationResponseFromIdp(idpEntityId, principalIpAddressAsSeenByHub);

        verify(eventSinkHubEventLogger).logPausedRegistrationEvent(
                NEW_SESSION_ID,
                transactionEntityId,
                sessionExpiryTimestamp,
                REQUEST_ID,
                principalIpAddressAsSeenByHub);
    }

    @Test
    public void handleRequesterErrorResponseFromIdp_shouldLogEvent() throws Exception {
        final String errorMessage = "an-error-message";
        RequesterErrorResponse requesterErrorResponse = aRequesterErrorResponse()
                .withIssuerId(idpEntityId)
                .withErrorMessage(errorMessage)
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));

        controller.handleRequesterErrorResponseFromIdp(requesterErrorResponse);

        verify(eventSinkHubEventLogger).logIdpRequesterErrorEvent(
                NEW_SESSION_ID,
                transactionEntityId,
                sessionExpiryTimestamp,
                REQUEST_ID,
                Optional.fromNullable(errorMessage),
                principalIpAddressAsSeenByHub);
    }

    @Test
    public void handleAuthnFailedResponseFromIdp_shouldLogEvent() throws Exception {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(idpEntityId)
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));

        controller.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);

        verify(eventSinkHubEventLogger).logIdpAuthnFailedEvent(
                NEW_SESSION_ID,
                transactionEntityId,
                sessionExpiryTimestamp,
                REQUEST_ID,
                principalIpAddressAsSeenByHub);
    }

    @Test
    public void handleNoAuthnContextResponseFromIdp_shouldLogEvent() throws Exception {
        AuthenticationErrorResponse authenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(idpEntityId)
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId));

        controller.handleNoAuthenticationContextResponseFromIdp(authenticationErrorResponse);

        verify(eventSinkHubEventLogger).logNoAuthnContextEvent(
                NEW_SESSION_ID,
                transactionEntityId,
                sessionExpiryTimestamp,
                REQUEST_ID,
                principalIpAddressAsSeenByHub);
    }

    @Test
    public void shouldReturnMatchingServiceEntityIdWhenAsked() throws Exception {
       controller.getMatchingServiceEntityId();
        verify(transactionsConfigProxy).getMatchingServiceEntityId(idpSelectedState.getRequestIssuerEntityId());
    }

    @Test(expected = StateProcessingValidationException.class)
    public void shouldThrowUnauditedErrorExceptionIfTheResponseIsFromADifferentIssuer(){
        PersistentId persistentId = aPersistentId().withNameId("idname").build();
        SuccessFromIdp successFromIdp = aSuccessFromIdp()
                .withIssuerId("differentIDP")
                .withPersistentId(persistentId)
                .withPrincipalIpAddressSeenByIdp(principalIpAddressAsSeenByIdp.get())
                .withPrincipalIpAddressAsSeenByHub(principalIpAddressAsSeenByHub)
                .withLevelOfAssurance(providedLevelOfAssurance)
                .build();
        when(identityProvidersConfigProxy.getEnabledIdentityProviders(fromNullable(transactionEntityId)))
                .thenReturn(asList(idpEntityId, "differentIDP"));

        controller.handleSuccessResponseFromIdp(successFromIdp);
    }
}
