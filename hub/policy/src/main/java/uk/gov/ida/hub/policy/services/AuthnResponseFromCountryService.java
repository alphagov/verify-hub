package uk.gov.ida.hub.policy.services;

import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.EidasCountrySelectedStateController;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;
import uk.gov.ida.hub.policy.metrics.EidasConnectorMetrics;

import javax.inject.Inject;
import java.util.List;

import static uk.gov.ida.hub.policy.domain.ResponseAction.nonMatchingJourneySuccess;
import static uk.gov.ida.hub.policy.domain.ResponseAction.other;
import static uk.gov.ida.hub.policy.domain.ResponseAction.success;

public class AuthnResponseFromCountryService {

    private final SamlEngineProxy samlEngineProxy;
    private final SamlSoapProxyProxy samlSoapProxyProxy;
    private final SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory;
    private final CountriesService countriesService;
    private final SessionRepository sessionRepository;

    @Inject
    public AuthnResponseFromCountryService(SamlEngineProxy samlEngineProxy,
                                           SamlSoapProxyProxy samlSoapProxyProxy,
                                           SessionRepository sessionRepository,
                                           SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory,
                                           CountriesService countriesService) {
        this.samlEngineProxy = samlEngineProxy;
        this.samlSoapProxyProxy = samlSoapProxyProxy;
        this.sessionRepository = sessionRepository;
        this.samlAuthnResponseTranslatorDtoFactory = samlAuthnResponseTranslatorDtoFactory;
        this.countriesService = countriesService;
    }

    public ResponseAction receiveAuthnResponseFromCountry(SessionId sessionId,
                                                          SamlAuthnResponseContainerDto responseFromCountry) {

        if (sessionRepository.sessionExists(sessionId) && sessionRepository.isSessionInState(sessionId, EidasAuthnFailedErrorState.class)) {
            return nonSuccessResponse(sessionId);
        }

        EidasCountrySelectedStateController stateController = (EidasCountrySelectedStateController) sessionRepository.getStateController(sessionId, EidasCountrySelectedState.class);
        validateCountryIsIn(countriesService.getCountries(sessionId), stateController.getCountryEntityId());

        boolean matchingJourney = stateController.isMatchingJourney();
        String entityToEncryptFor = matchingJourney ? stateController.getMatchingServiceEntityId() : stateController.getRequestIssuerId();

        SamlAuthnResponseTranslatorDto responseToTranslate = samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(responseFromCountry, entityToEncryptFor);
        InboundResponseFromCountry translatedResponse = samlEngineProxy.translateAuthnResponseFromCountry(responseToTranslate);

        return handleResponseFromCountry(translatedResponse, responseFromCountry, sessionId, matchingJourney, stateController);
    }

    private ResponseAction handleResponseFromCountry(InboundResponseFromCountry translatedResponse, SamlAuthnResponseContainerDto responseFromCountry, SessionId sessionId, boolean matchingJourney, EidasCountrySelectedStateController stateController) {
        ResponseAction responseAction;
        switch (translatedResponse.getStatus()) {
            case Success:
                responseAction = handleSuccessResponse(translatedResponse, responseFromCountry, sessionId, matchingJourney, stateController);
                EidasConnectorMetrics.increment(translatedResponse.getIssuer(), EidasConnectorMetrics.Direction.response, EidasConnectorMetrics.Status.ok);
                break;
            case Failure:
                responseAction = handleAuthenticationFailedResponse(responseFromCountry, sessionId, stateController);
                EidasConnectorMetrics.increment(translatedResponse.getIssuer(), EidasConnectorMetrics.Direction.response, EidasConnectorMetrics.Status.error);
                break;
            default:
                responseAction = nonSuccessResponse(sessionId);
                EidasConnectorMetrics.increment(translatedResponse.getIssuer(), EidasConnectorMetrics.Direction.response, EidasConnectorMetrics.Status.ko);
                break;
        }

        return responseAction;
    }

    private ResponseAction handleSuccessResponse(InboundResponseFromCountry translatedResponse,
                                                 SamlAuthnResponseContainerDto responseFromCountry,
                                                 SessionId sessionId,
                                                 boolean matchingJourney,
                                                 EidasCountrySelectedStateController stateController) {
        if (matchingJourney) {
            stateController.handleMatchingJourneySuccessResponseFromCountry(translatedResponse,
                    responseFromCountry.getPrincipalIPAddressAsSeenByHub(),
                    responseFromCountry.getAnalyticsSessionId(),
                    responseFromCountry.getJourneyType());
            AttributeQueryContainerDto aqr = samlEngineProxy.generateEidasAttributeQuery(stateController.getEidasAttributeQueryRequestDto(translatedResponse));
            samlSoapProxyProxy.sendHubMatchingServiceRequest(sessionId, getAttributeQueryRequest(aqr));
            return success(sessionId, false, LevelOfAssurance.LEVEL_2, null);
        } else {
            stateController.handleNonMatchingJourneySuccessResponseFromCountry(translatedResponse,
                    responseFromCountry.getPrincipalIPAddressAsSeenByHub(),
                    responseFromCountry.getAnalyticsSessionId(),
                    responseFromCountry.getJourneyType());
            return nonMatchingJourneySuccess(sessionId, false, LevelOfAssurance.LEVEL_2, null);
        }
    }

    private ResponseAction handleAuthenticationFailedResponse(SamlAuthnResponseContainerDto responseFromCountry,
                                                              SessionId sessionId,
                                                              EidasCountrySelectedStateController stateController) {
        stateController.handleAuthenticationFailedResponseFromCountry(responseFromCountry.getPrincipalIPAddressAsSeenByHub(), responseFromCountry.getAnalyticsSessionId(), responseFromCountry.getJourneyType());

        return nonSuccessResponse(sessionId);
    }

    private ResponseAction nonSuccessResponse(SessionId sessionId) {
        return other(sessionId, false);
    }

    private AttributeQueryRequest getAttributeQueryRequest(AttributeQueryContainerDto aqr) {
        return new AttributeQueryRequest(aqr.getId(), aqr.getIssuer(), aqr.getSamlRequest(), aqr.getMatchingServiceUri(), aqr.getAttributeQueryClientTimeOut(), aqr.isOnboarding());
    }

    private void validateCountryIsIn(List<EidasCountryDto> countries, String countryEntityId) {
        if (countries.stream().noneMatch(c -> countryEntityId.equals(c.getEntityId()))) {
            throw StateProcessingValidationException.eidasCountryNotEnabled(countryEntityId);
        }
    }
}
