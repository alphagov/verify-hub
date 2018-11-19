package uk.gov.ida.hub.policy.services;

import uk.gov.ida.hub.policy.contracts.AttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.domain.AuthenticationErrorResponse;
import uk.gov.ida.hub.policy.domain.FraudDetectedDetails;
import uk.gov.ida.hub.policy.domain.FraudFromIdp;
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

import javax.inject.Inject;

import static uk.gov.ida.hub.policy.domain.ResponseAction.cancel;
import static uk.gov.ida.hub.policy.domain.ResponseAction.failedUplift;
import static uk.gov.ida.hub.policy.domain.ResponseAction.other;
import static uk.gov.ida.hub.policy.domain.ResponseAction.pending;
import static uk.gov.ida.hub.policy.domain.ResponseAction.success;
import static uk.gov.ida.hub.policy.domain.ResponseAction.nonMatchingJourneySuccess;


public class AuthnResponseFromIdpService {
    private final SamlEngineProxy samlEngineProxy;
    private final AttributeQueryService attributeQueryService;
    private final SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory;
    private SessionRepository sessionRepository;

    @Inject
    public AuthnResponseFromIdpService(SamlEngineProxy samlEngineProxy,
                                       AttributeQueryService attributeQueryService,
                                       SessionRepository sessionRepository,
                                       SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory) {
        this.samlEngineProxy = samlEngineProxy;
        this.attributeQueryService = attributeQueryService;
        this.sessionRepository = sessionRepository;
        this.samlAuthnResponseTranslatorDtoFactory = samlAuthnResponseTranslatorDtoFactory;
    }

    public ResponseAction receiveAuthnResponseFromIdp(SessionId sessionId,
                                                      SamlAuthnResponseContainerDto samlResponseDto) {

        IdpSelectedStateController idpSelectedController = (IdpSelectedStateController) sessionRepository.getStateController(sessionId, IdpSelectedState.class);

        String matchingServiceEntityId = idpSelectedController.getMatchingServiceEntityId();
        final SamlAuthnResponseTranslatorDto samlAuthnResponseTranslatorDto = samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(samlResponseDto, matchingServiceEntityId);
        final InboundResponseFromIdpDto idaResponseFromIdpDto = samlEngineProxy.translateAuthnResponseFromIdp(samlAuthnResponseTranslatorDto);
        final String principalIPAddressAsSeenByHub = samlResponseDto.getPrincipalIPAddressAsSeenByHub();

        ResponseAction responseAction;

        if (isFraudulent(idaResponseFromIdpDto)) {
            responseAction = handleFraudResponse(idaResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
        } else {
            responseAction = handleNonFraudResponse(idaResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
        }
        return responseAction;
    }

    private boolean isFraudulent(InboundResponseFromIdpDto idaResponseFromIdpDto) {
        return idaResponseFromIdpDto.getLevelOfAssurance().isPresent() &&
                idaResponseFromIdpDto.getLevelOfAssurance().get().equals(LevelOfAssurance.LEVEL_X);
    }

    private ResponseAction handleNonFraudResponse(InboundResponseFromIdpDto inboundResponseFromIdpDto, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedController) {
        ResponseAction responseAction;
        switch (inboundResponseFromIdpDto.getStatus()) {
            case NoAuthenticationContext:
                responseAction = handleNoAuthnContextResponse(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            case AuthenticationCancelled:
                responseAction = handleCancelResponse(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            case AuthenticationFailed:
                responseAction = handleAuthnFailedResponse(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            case RequesterError:
                responseAction = handleRequesterError(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            case UpliftFailed:
                responseAction = handleUpliftFailed(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            case Success:
                responseAction = handleSuccessResponse(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            case AuthenticationPending:
                responseAction = handlePendingResponse(inboundResponseFromIdpDto, sessionId, principalIPAddressAsSeenByHub, idpSelectedController);
                break;
            default:
                throw new UnsupportedOperationException("We don't support any of the other response types - should probably fix this");
        }
        return responseAction;
    }

    private ResponseAction handleSuccessResponse(InboundResponseFromIdpDto inboundResponseFromIdpDto, SessionId sessionId,
                                                 String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        LevelOfAssurance loaAchieved = inboundResponseFromIdpDto.getLevelOfAssurance().get();
        SuccessFromIdp successFromIdp = new SuccessFromIdp(
                inboundResponseFromIdpDto.getIssuer(),
                inboundResponseFromIdpDto.getEncryptedMatchingDatasetAssertion().get(),
                inboundResponseFromIdpDto.getEncryptedAuthnAssertion().get(),
                new PersistentId(inboundResponseFromIdpDto.getPersistentId().get()),
                loaAchieved,
                principalIPAddressAsSeenByHub,
                inboundResponseFromIdpDto.getPrincipalIpAddressAsSeenByIdp());

        if (idpSelectedStateController.isMatchingJourney()) {
            idpSelectedStateController.handleMatchingJourneySuccessResponseFromIdp(successFromIdp);
            AttributeQueryRequestDto attributeQuery = idpSelectedStateController.createAttributeQuery(successFromIdp);
            attributeQueryService.sendAttributeQueryRequest(sessionId, attributeQuery);
            return success(sessionId, idpSelectedStateController.isRegistrationContext(), loaAchieved);
        } else {
            idpSelectedStateController.handleNonMatchingJourneySuccessResponseFromIdp(successFromIdp);
            return nonMatchingJourneySuccess(sessionId, idpSelectedStateController.isRegistrationContext(), loaAchieved);
        }
    }

    private ResponseAction handleRequesterError(InboundResponseFromIdpDto idaResponseFromIdp, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        RequesterErrorResponse requesterErrorResponse =
                new RequesterErrorResponse(idaResponseFromIdp.getIssuer(), idaResponseFromIdp.getStatusMessage(), principalIPAddressAsSeenByHub);
        idpSelectedStateController.handleRequesterErrorResponseFromIdp(requesterErrorResponse);
        return other(sessionId, idpSelectedStateController.isRegistrationContext());
    }

    private ResponseAction handleAuthnFailedResponse(InboundResponseFromIdpDto idaResponseFromIdp, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        AuthenticationErrorResponse authenticationErrorResponse =
                new AuthenticationErrorResponse(idaResponseFromIdp.getIssuer(), principalIPAddressAsSeenByHub);
        idpSelectedStateController.handleAuthenticationFailedResponseFromIdp(authenticationErrorResponse);
        return other(sessionId, idpSelectedStateController.isRegistrationContext());
    }

    private ResponseAction handleNoAuthnContextResponse(InboundResponseFromIdpDto idaResponseFromIdp, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        AuthenticationErrorResponse noAuthenticationContextErrorResponse =
                new AuthenticationErrorResponse(idaResponseFromIdp.getIssuer(), principalIPAddressAsSeenByHub);
        idpSelectedStateController.handleNoAuthenticationContextResponseFromIdp(noAuthenticationContextErrorResponse);
        return other(sessionId, idpSelectedStateController.isRegistrationContext());
    }

    private ResponseAction handleCancelResponse(InboundResponseFromIdpDto idaResponseFromIdp, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        AuthenticationErrorResponse noAuthenticationContextErrorResponse =
                new AuthenticationErrorResponse(idaResponseFromIdp.getIssuer(), principalIPAddressAsSeenByHub);
        idpSelectedStateController.handleNoAuthenticationContextResponseFromIdp(noAuthenticationContextErrorResponse);
        return cancel(sessionId, idpSelectedStateController.isRegistrationContext());
    }

    private ResponseAction handleFraudResponse(InboundResponseFromIdpDto inboundResponseFromIdpDto, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        FraudFromIdp fraudFromIdp = new FraudFromIdp(
                        inboundResponseFromIdpDto.getIssuer(),
                        principalIPAddressAsSeenByHub,
                        new PersistentId(inboundResponseFromIdpDto.getPersistentId().get()),
                        new FraudDetectedDetails(inboundResponseFromIdpDto.getIdpFraudEventId().get(), inboundResponseFromIdpDto.getFraudIndicator().get()),
                        inboundResponseFromIdpDto.getPrincipalIpAddressAsSeenByIdp());
        idpSelectedStateController.handleFraudResponseFromIdp(fraudFromIdp);
        return other(sessionId, idpSelectedStateController.isRegistrationContext());
    }

    private ResponseAction handleUpliftFailed(InboundResponseFromIdpDto inboundResponseFromIdpDto, SessionId sessionId, String principalIPAddressAsSeenByHub, IdpSelectedStateController idpSelectedController) {
        AuthenticationErrorResponse noAuthenticationContextErrorResponse =
                new AuthenticationErrorResponse(inboundResponseFromIdpDto.getIssuer(), principalIPAddressAsSeenByHub);
        idpSelectedController.handleNoAuthenticationContextResponseFromIdp(noAuthenticationContextErrorResponse);
        return failedUplift(sessionId, idpSelectedController.isRegistrationContext());
    }

    private ResponseAction handlePendingResponse(InboundResponseFromIdpDto inboundResponseFromIdpDto, SessionId sessionId, String principalIpAddressAsSeenByHub, IdpSelectedStateController idpSelectedStateController) {
        idpSelectedStateController.handlePausedRegistrationResponseFromIdp(inboundResponseFromIdpDto.getIssuer(), principalIpAddressAsSeenByHub, inboundResponseFromIdpDto.getLevelOfAssurance().toJavaUtil());
        return pending(sessionId);
    }
}

