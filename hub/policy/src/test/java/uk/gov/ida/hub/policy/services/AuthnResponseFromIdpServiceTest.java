package uk.gov.ida.hub.policy.services;

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
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.policy.builder.SamlAuthnResponseContainerDtoBuilder.aSamlAuthnResponseContainerDto;
import static uk.gov.ida.hub.policy.builder.domain.AuthenticationErrorResponseBuilder.anAuthenticationErrorResponse;
import static uk.gov.ida.hub.policy.builder.domain.RequesterErrorResponseBuilder.aRequesterErrorResponse;

@RunWith(MockitoJUnitRunner.class)
public class AuthnResponseFromIdpServiceTest {

    @Mock
    private SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory;
    @Mock
    private SamlEngineProxy samlEngineProxy;
    @Mock
    private IdpSelectedStateController idpSelectedStateController;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AttributeQueryService attributeQueryService;

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
        sessionId = SessionIdBuilder.aSessionId().build();
        samlAuthnResponseContainerDto = aSamlAuthnResponseContainerDto().withSessionId(sessionId).withPrincipalIPAddressAsSeenByHub(
                PRINCIPAL_IP_ADDRESS).withAnalyticsSessionId(ANALYTICS_SESSION_ID).withJourneyType(JOURNEY_TYPE).build();
        service = new AuthnResponseFromIdpService(samlEngineProxy, attributeQueryService, sessionRepository, samlAuthnResponseTranslatorDtoFactory);
        when(sessionRepository.getStateController(sessionId, IdpSelectedState.class)).thenReturn(idpSelectedStateController);
        when(sessionRepository.getCurrentState(sessionId)).thenReturn(mock(IdpSelectedState.class));
    }

    @Test
    public void shouldSendRequestToMatchingServiceViaAttributeQueryServiceAndUpdateSessionStateWhenSuccessfulResponseIsReceivedForAServiceWithMatching() {
        // Given
        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        InboundResponseFromIdpDto successResponseFromIdp = InboundResponseFromIdpDtoBuilder.successResponse(UUID.randomUUID().toString(), loaAchieved, null);
        mockOutStubs(REGISTERING, true, successResponseFromIdp);

        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        when(idpSelectedStateController.createAttributeQuery(any(SuccessFromIdp.class))).thenReturn(attributeQueryRequestDto);
        AttributeQueryContainerDto msaRequest = AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build();

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlAuthnResponseTranslatorDtoFactory).fromSamlAuthnResponseContainerDto(samlAuthnResponseContainerDto, MSA_ENTITY_ID);
        verify(attributeQueryService).sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);
        verifyIdpStateControllerIsCalledWithRightDataOnSuccess(successResponseFromIdp, true);
        ResponseAction expectedResponseAction = ResponseAction.success(sessionId, REGISTERING, loaAchieved, null);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
    }

    @Test
    public void shouldReturnNonMatchingJourneySuccessWhenSuccessfulResponseIsReceivedForAServiceWithNonMatching() {
        // Given
        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        InboundResponseFromIdpDto successResponseFromIdp = InboundResponseFromIdpDtoBuilder.successResponse(UUID.randomUUID().toString(), loaAchieved, null);
        mockOutStubs(REGISTERING, false, successResponseFromIdp);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verifyIdpStateControllerIsCalledWithRightDataOnSuccess(successResponseFromIdp, false);
        ResponseAction expectedResponseAction = ResponseAction.nonMatchingJourneySuccess(sessionId, REGISTERING, loaAchieved, null);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
    }

    @Test
    public void shouldOnlyUpdateSessionStateWhenAFraudSuccessfulResponseIsReceived() {
        // Given
        InboundResponseFromIdpDto fraudResponseFromIdp = InboundResponseFromIdpDtoBuilder.fraudResponse(UUID.randomUUID().toString());
        mockOutStubs(REGISTERING, false, fraudResponseFromIdp);
        when(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).thenReturn(fraudResponseFromIdp);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));
        ResponseAction expectedResponseAction = ResponseAction.other(sessionId, REGISTERING);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);

        verifyIdpStateControllerIsCalledWithRightDataOnFraud(fraudResponseFromIdp);
    }

    @Test
    public void shouldOnlyUpdateSessionStateWhenANonFraudRequesterErrorResponseIsReceived() {
        // Given
        InboundResponseFromIdpDto requesterErrorResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.RequesterError);
        mockOutStubs(REGISTERING, false, requesterErrorResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));
        verify(idpSelectedStateController).handleRequesterErrorResponseFromIdp(any(RequesterErrorResponse.class));
        ResponseAction expectedResponseAction = ResponseAction.other(sessionId, REGISTERING);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);

        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudRequesterError(requesterErrorResponse);
    }

    @Test
    public void shouldOnlyUpdateSessionStateWhenANonFraudRequesterPendingResponseIsReceived() {
        // Given
        String entityId = UUID.randomUUID().toString();
        InboundResponseFromIdpDto authnPendingResponse = InboundResponseFromIdpDtoBuilder.authnPendingResponse(entityId);
        mockOutStubs(REGISTERING, false, authnPendingResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(idpSelectedStateController).handlePausedRegistrationResponseFromIdp(entityId, PRINCIPAL_IP_ADDRESS, authnPendingResponse.getLevelOfAssurance().toJavaUtil(), ANALYTICS_SESSION_ID, JOURNEY_TYPE);
        ResponseAction expectedResponseAction = ResponseAction.pending(sessionId);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
    }

    @Test
    public void shouldOnlyUpdateSessionStateWhenANonFraudAuthenticationFailedResponseIsReceived() {
        // Given
        InboundResponseFromIdpDto authenticationFailedResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.AuthenticationFailed);
        mockOutStubs(REGISTERING, false, authenticationFailedResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));

        verifyNoMoreInteractions(samlEngineProxy);
        ResponseAction expectedResponseAction = ResponseAction.other(sessionId, REGISTERING);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);

        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudAuthenticationFailed(authenticationFailedResponse);
    }

    @Test
    public void mapAuthnContextResponseFromIdpAsOther() {
        // Given
        InboundResponseFromIdpDto noAuthenticationContextResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.NoAuthenticationContext);
        mockOutStubs(REGISTERING, false, noAuthenticationContextResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));

        verifyNoMoreInteractions(samlEngineProxy);
        verify(idpSelectedStateController).handleNoAuthenticationContextResponseFromIdp(any(AuthenticationErrorResponse.class));
        ResponseAction expectedResponseAction = ResponseAction.other(sessionId, REGISTERING);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudNoAuthenticationContext(noAuthenticationContextResponse);
    }

    @Test
    public void mapAuthnCancelResponseFromIDP() {
        // Given
        InboundResponseFromIdpDto noAuthenticationContextResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.AuthenticationCancelled);
        mockOutStubs(REGISTERING, false, noAuthenticationContextResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));

        verifyNoMoreInteractions(samlEngineProxy);
        verify(idpSelectedStateController).handleNoAuthenticationContextResponseFromIdp(any(AuthenticationErrorResponse.class));

        ResponseAction expectedResponseAction = ResponseAction.cancel(sessionId, REGISTERING);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);

        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudNoAuthenticationContext(noAuthenticationContextResponse);
    }

    @Test
    public void mapFailedUpliftResponseFromIDP() {
        // Given
        InboundResponseFromIdpDto noAuthenticationContextResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.UpliftFailed);
        mockOutStubs(REGISTERING, false, noAuthenticationContextResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));

        verifyNoMoreInteractions(samlEngineProxy);
        verify(idpSelectedStateController).handleNoAuthenticationContextResponseFromIdp(any(AuthenticationErrorResponse.class));
        ResponseAction expectedResponseAction = ResponseAction.failedUplift(sessionId, REGISTERING);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudNoAuthenticationContext(noAuthenticationContextResponse);
    }

    private void mockOutStubs(boolean isRegistering, boolean isMatchingJourney, InboundResponseFromIdpDto responseFromIdpDto) {
        when(idpSelectedStateController.isRegistrationContext()).thenReturn(isRegistering);
        when(idpSelectedStateController.getMatchingServiceEntityId()).thenReturn(MSA_ENTITY_ID);
        when(idpSelectedStateController.getRequestIssuerId()).thenReturn(REQUEST_ISSUER_ID);
        when(idpSelectedStateController.isMatchingJourney()).thenReturn(isMatchingJourney);
        SamlAuthnResponseTranslatorDto samlAuthnResponseTranslatorDto = SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto().build();
        when(samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(samlAuthnResponseContainerDto, MSA_ENTITY_ID)).thenReturn(samlAuthnResponseTranslatorDto);

        if (isMatchingJourney) {
            when(samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(samlAuthnResponseContainerDto, MSA_ENTITY_ID)).thenReturn(samlAuthnResponseTranslatorDto);
        } else {
            when(samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(samlAuthnResponseContainerDto, REQUEST_ISSUER_ID)).thenReturn(samlAuthnResponseTranslatorDto);
        }
        when(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).thenReturn(responseFromIdpDto);
    }

        private void verifyIdpStateControllerIsCalledWithRightDataOnFraud(InboundResponseFromIdpDto fraudResponseFromIdp) {
        ArgumentCaptor<FraudFromIdp> captor = ArgumentCaptor.forClass(FraudFromIdp.class);

        String persistentIdName = fraudResponseFromIdp.getPersistentId().get();
        FraudDetectedDetails expectedFraudDetectedDetails = new FraudDetectedDetails(fraudResponseFromIdp.getIdpFraudEventId().get(), fraudResponseFromIdp.getFraudIndicator().get());
        FraudFromIdp fraudFromIdp = new FraudFromIdp(
                fraudResponseFromIdp.getIssuer(),
                samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub(),
                new PersistentId(persistentIdName),
                expectedFraudDetectedDetails,
                fraudResponseFromIdp.getPrincipalIpAddressAsSeenByIdp(),
                ANALYTICS_SESSION_ID,
                JOURNEY_TYPE);

        verify(idpSelectedStateController).handleFraudResponseFromIdp(captor.capture());
        FraudFromIdp actualFraudFromIdp = captor.getValue();
        assertThat(actualFraudFromIdp).isEqualToIgnoringGivenFields(fraudFromIdp, "persistentId", "fraudDetectedDetails");
        assertThat(actualFraudFromIdp.getPersistentId().getNameId()).isEqualTo(persistentIdName);
        assertThat(actualFraudFromIdp.getFraudDetectedDetails()).isEqualToComparingFieldByField(expectedFraudDetectedDetails);
    }

    private void verifyIdpStateControllerIsCalledWithRightDataOnNonFraudRequesterError(InboundResponseFromIdpDto requesterErrorResponse) {
        ArgumentCaptor<RequesterErrorResponse> captor = ArgumentCaptor.forClass(RequesterErrorResponse.class);

        RequesterErrorResponse expectedRequesterErrorResponse = aRequesterErrorResponse()
                .withIssuerId(requesterErrorResponse.getIssuer())
                .withErrorMessage(requesterErrorResponse.getStatusMessage().get())
                .withPrincipalIpAddressAsSeenByHub(samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub()).build();

        verify(idpSelectedStateController).handleRequesterErrorResponseFromIdp(captor.capture());
        RequesterErrorResponse actualRequesterErrorResponse = captor.getValue();
        assertThat(actualRequesterErrorResponse).isEqualToIgnoringGivenFields(expectedRequesterErrorResponse);
    }

    private void verifyIdpStateControllerIsCalledWithRightDataOnNonFraudAuthenticationFailed(InboundResponseFromIdpDto authenticationFailedResponse) {
        ArgumentCaptor<AuthenticationErrorResponse> captor = ArgumentCaptor.forClass(AuthenticationErrorResponse.class);

        AuthenticationErrorResponse expectedAuthenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(authenticationFailedResponse.getIssuer())
                .withPrincipalIpAddressAsSeenByHub(samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub())
                .withAnalyticsSessionId(ANALYTICS_SESSION_ID)
                .withJourneyType(JOURNEY_TYPE)
                .build();

        verify(idpSelectedStateController).handleAuthenticationFailedResponseFromIdp(captor.capture());
        AuthenticationErrorResponse actualAuthenticationErrorResponse = captor.getValue();
        assertThat(actualAuthenticationErrorResponse).isEqualToIgnoringGivenFields(expectedAuthenticationErrorResponse);
    }

    private void verifyIdpStateControllerIsCalledWithRightDataOnSuccess(InboundResponseFromIdpDto successResponseFromIdp, boolean forMatchingJourney) {
        ArgumentCaptor<SuccessFromIdp> captor = ArgumentCaptor.forClass(SuccessFromIdp.class);

        PersistentId persistentId = PersistentIdBuilder.aPersistentId().withNameId(successResponseFromIdp.getPersistentId().get()).build();
        SuccessFromIdp expectedSuccessFromIdp = SuccessFromIdpBuilder.aSuccessFromIdp()
                .withIssuerId(successResponseFromIdp.getIssuer())
                .withEncryptedMatchingDatasetAssertion(successResponseFromIdp.getEncryptedMatchingDatasetAssertion().get())
                .withAuthnStatementAssertion(successResponseFromIdp.getEncryptedAuthnAssertion().get())
                .withPersistentId(persistentId)
                .withLevelOfAssurance(successResponseFromIdp.getLevelOfAssurance().get())
                .withPrincipalIpAddressAsSeenByHub(samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub())
                .withPrincipalIpAddressSeenByIdp(successResponseFromIdp.getPrincipalIpAddressAsSeenByIdp().get())
                .withAnalyticsSessionId(ANALYTICS_SESSION_ID)
                .withJourneyType(JOURNEY_TYPE)
                .build();

        if (forMatchingJourney) {
            verify(idpSelectedStateController).handleMatchingJourneySuccessResponseFromIdp(captor.capture());
        } else {
            verify(idpSelectedStateController).handleNonMatchingJourneySuccessResponseFromIdp(captor.capture());
        }

        SuccessFromIdp actualSuccessFromIdp = captor.getValue();
        assertThat(actualSuccessFromIdp).isEqualToIgnoringGivenFields(expectedSuccessFromIdp, "persistentId");
        assertThat(actualSuccessFromIdp.getPersistentId().getNameId()).isEqualTo(persistentId.getNameId());
    }

    private void verifyIdpStateControllerIsCalledWithRightDataOnNonFraudNoAuthenticationContext(InboundResponseFromIdpDto noAuthenticationContext) {
        ArgumentCaptor<AuthenticationErrorResponse> captor = ArgumentCaptor.forClass(AuthenticationErrorResponse.class);

        AuthenticationErrorResponse expectedAuthenticationErrorResponse = anAuthenticationErrorResponse()
                .withIssuerId(noAuthenticationContext.getIssuer())
                .withPrincipalIpAddressAsSeenByHub(samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub()).build();

        verify(idpSelectedStateController).handleNoAuthenticationContextResponseFromIdp(captor.capture());
        AuthenticationErrorResponse actualAuthenticationErrorResponse = captor.getValue();
        assertThat(actualAuthenticationErrorResponse).isEqualToIgnoringGivenFields(expectedAuthenticationErrorResponse);
    }
}
