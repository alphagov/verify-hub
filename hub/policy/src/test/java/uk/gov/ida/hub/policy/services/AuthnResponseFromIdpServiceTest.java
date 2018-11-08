package uk.gov.ida.hub.policy.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.stub;
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

    @Before
    public void setup() {
        sessionId = SessionIdBuilder.aSessionId().build();
        samlAuthnResponseContainerDto = aSamlAuthnResponseContainerDto().withSessionId(sessionId).withPrincipalIPAddressAsSeenByHub(PRINCIPAL_IP_ADDRESS).build();
        service = new AuthnResponseFromIdpService(samlEngineProxy, attributeQueryService, sessionRepository, samlAuthnResponseTranslatorDtoFactory);
        when(sessionRepository.getStateController(sessionId, IdpSelectedState.class)).thenReturn(idpSelectedStateController);
    }

    @Test
    public void shouldSendRequestToMatchingServiceViaAttributeQueryServiceAndUpdateSessionStateWhenSuccessfulResponseIsReceived() {
        // Given
        final String msaEntityId = "a-msa-entity-id";
        LevelOfAssurance loaAchieved = LevelOfAssurance.LEVEL_2;
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(REGISTERING);
        when(idpSelectedStateController.getMatchingServiceEntityId()).thenReturn(msaEntityId);
        InboundResponseFromIdpDto successResponseFromIdp = InboundResponseFromIdpDtoBuilder.successResponse(UUID.randomUUID().toString(), loaAchieved);
        SamlAuthnResponseTranslatorDto samlAuthnResponseTranslatorDto = SamlAuthnResponseTranslatorDtoBuilder.aSamlAuthnResponseTranslatorDto().build();
        when(samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(samlAuthnResponseContainerDto, msaEntityId)).thenReturn(samlAuthnResponseTranslatorDto);
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(successResponseFromIdp);
        AttributeQueryRequestDto attributeQueryRequestDto = AttributeQueryRequestBuilder.anAttributeQueryRequest().build();
        stub(idpSelectedStateController.createAttributeQuery(any(SuccessFromIdp.class))).toReturn(attributeQueryRequestDto);
        AttributeQueryContainerDto msaRequest = AttributeQueryContainerDtoBuilder.anAttributeQueryContainerDto().build();
        stub(samlEngineProxy.generateAttributeQuery(attributeQueryRequestDto)).toReturn(msaRequest);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlAuthnResponseTranslatorDtoFactory).fromSamlAuthnResponseContainerDto(samlAuthnResponseContainerDto, msaEntityId);
        verify(attributeQueryService).sendAttributeQueryRequest(sessionId, attributeQueryRequestDto);
        verifyIdpStateControllerIsCalledWithRightDataOnSuccess(successResponseFromIdp);
        ResponseAction expectedResponseAction = ResponseAction.success(sessionId, REGISTERING, loaAchieved);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
    }

    @Test
    public void shouldOnlyUpdateSessionStateWhenAFraudSuccessfulResponseIsReceived() {
        // Given
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(REGISTERING);
        InboundResponseFromIdpDto fraudResponseFromIdp = InboundResponseFromIdpDtoBuilder.fraudResponse(UUID.randomUUID().toString());
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(fraudResponseFromIdp);

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
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(REGISTERING);
        InboundResponseFromIdpDto requesterErrorResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.RequesterError);
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(requesterErrorResponse);

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
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(authnPendingResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(idpSelectedStateController).handlePausedRegistrationResponseFromIdp(entityId, PRINCIPAL_IP_ADDRESS, authnPendingResponse.getLevelOfAssurance().toJavaUtil());
        ResponseAction expectedResponseAction = ResponseAction.pending(sessionId);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
    }

    @Test
    public void shouldOnlyUpdateSessionStateWhenANonFraudAuthenticationFailedResponseIsReceived() {
        // Given
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(REGISTERING);
        InboundResponseFromIdpDto authenticationFailedResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.AuthenticationFailed);
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(authenticationFailedResponse);

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
        final boolean isRegistration = true;
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(isRegistration);
        InboundResponseFromIdpDto noAuthenticationContextResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.NoAuthenticationContext);
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(noAuthenticationContextResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));

        verifyNoMoreInteractions(samlEngineProxy);
        verify(idpSelectedStateController).handleNoAuthenticationContextResponseFromIdp(any(AuthenticationErrorResponse.class));
        ResponseAction expectedResponseAction = ResponseAction.other(sessionId, isRegistration);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);
        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudNoAuthenticationContext(noAuthenticationContextResponse);
    }

    @Test
    public void mapAuthnCancelResponseFromIDP() {
        // Given
        final boolean isRegistration = true;
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(isRegistration);
        InboundResponseFromIdpDto noAuthenticationContextResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.AuthenticationCancelled);
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(noAuthenticationContextResponse);

        // When
        ResponseAction responseAction = service.receiveAuthnResponseFromIdp(sessionId, samlAuthnResponseContainerDto);

        // Then
        verify(samlEngineProxy).translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class));

        verifyNoMoreInteractions(samlEngineProxy);
        verify(idpSelectedStateController).handleNoAuthenticationContextResponseFromIdp(any(AuthenticationErrorResponse.class));

        ResponseAction expectedResponseAction = ResponseAction.cancel(sessionId, isRegistration);
        assertThat(responseAction).isEqualToComparingFieldByField(expectedResponseAction);

        verifyIdpStateControllerIsCalledWithRightDataOnNonFraudNoAuthenticationContext(noAuthenticationContextResponse);
    }

    @Test
    public void mapFailedUpliftResponseFromIDP() {
        // Given
        stub(idpSelectedStateController.isRegistrationContext()).toReturn(REGISTERING);
        InboundResponseFromIdpDto noAuthenticationContextResponse = InboundResponseFromIdpDtoBuilder.errorResponse(UUID.randomUUID().toString(), IdpIdaStatus.Status.UpliftFailed);
        stub(samlEngineProxy.translateAuthnResponseFromIdp(any(SamlAuthnResponseTranslatorDto.class))).toReturn(noAuthenticationContextResponse);

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

    private void verifyIdpStateControllerIsCalledWithRightDataOnFraud(InboundResponseFromIdpDto fraudResponseFromIdp) {
        ArgumentCaptor<FraudFromIdp> captor = ArgumentCaptor.forClass(FraudFromIdp.class);

        String persistentIdName = fraudResponseFromIdp.getPersistentId().get();
        FraudDetectedDetails expectedFraudDetectedDetails = new FraudDetectedDetails(fraudResponseFromIdp.getIdpFraudEventId().get(), fraudResponseFromIdp.getFraudIndicator().get());
        FraudFromIdp fraudFromIdp = new FraudFromIdp(
                fraudResponseFromIdp.getIssuer(),
                samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub(),
                new PersistentId(persistentIdName),
                expectedFraudDetectedDetails,
                fraudResponseFromIdp.getPrincipalIpAddressAsSeenByIdp());

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
                .withPrincipalIpAddressAsSeenByHub(samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub()).build();

        verify(idpSelectedStateController).handleAuthenticationFailedResponseFromIdp(captor.capture());
        AuthenticationErrorResponse actualAuthenticationErrorResponse = captor.getValue();
        assertThat(actualAuthenticationErrorResponse).isEqualToIgnoringGivenFields(expectedAuthenticationErrorResponse);
    }

    private void verifyIdpStateControllerIsCalledWithRightDataOnSuccess(InboundResponseFromIdpDto successResponseFromIdp) {
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
                .build();
        verify(idpSelectedStateController).handleSuccessResponseFromIdp(captor.capture());
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
