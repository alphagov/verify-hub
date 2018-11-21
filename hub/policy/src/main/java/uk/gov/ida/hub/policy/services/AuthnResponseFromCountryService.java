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
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;

import javax.inject.Inject;
import java.util.List;

import static uk.gov.ida.hub.policy.domain.ResponseAction.other;

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

        EidasCountrySelectedStateController stateController = (EidasCountrySelectedStateController) sessionRepository.getStateController(sessionId, EidasCountrySelectedState.class);
        String matchingServiceEntityId = stateController.getMatchingServiceEntityId();
        validateCountryIsIn(countriesService.getCountries(sessionId), stateController.getCountryEntityId());

        SamlAuthnResponseTranslatorDto responseToTranslate = samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(responseFromCountry, matchingServiceEntityId);
        InboundResponseFromCountry translatedResponse = samlEngineProxy.translateAuthnResponseFromCountry(responseToTranslate);

        return handleResponseFromCountry(translatedResponse, responseFromCountry, sessionId, stateController);
    }

    private ResponseAction handleResponseFromCountry(InboundResponseFromCountry translatedResponse, SamlAuthnResponseContainerDto responseFromCountry, SessionId sessionId, EidasCountrySelectedStateController stateController) {
        ResponseAction responseAction;
        switch (translatedResponse.getStatus()) {
            case Success:
                responseAction = handleSuccessResponse(translatedResponse, responseFromCountry, sessionId, stateController);
                break;
            case Failure:
                responseAction = handleAuthenticationFailedResponse(responseFromCountry, sessionId, stateController);
                break;
            default:
                responseAction = handleOtherResponse(sessionId);
                break;
        }

        return responseAction;
    }

    private ResponseAction handleSuccessResponse(InboundResponseFromCountry translatedResponse, SamlAuthnResponseContainerDto responseFromCountry, SessionId sessionId, EidasCountrySelectedStateController stateController) {
        stateController.handleSuccessResponseFromCountry(translatedResponse, responseFromCountry.getPrincipalIPAddressAsSeenByHub());

        AttributeQueryContainerDto aqr = samlEngineProxy.generateEidasAttributeQuery(stateController.getEidasAttributeQueryRequestDto(translatedResponse));
        samlSoapProxyProxy.sendHubMatchingServiceRequest(sessionId, getAttributeQueryRequest(aqr));

        return ResponseAction.success(sessionId, false, LevelOfAssurance.LEVEL_2, null);
    }

    private ResponseAction handleAuthenticationFailedResponse(SamlAuthnResponseContainerDto responseFromCountry, SessionId sessionId, EidasCountrySelectedStateController stateController) {
        stateController.handleAuthenticationFailedResponseFromCountry(responseFromCountry.getPrincipalIPAddressAsSeenByHub());

        return other(sessionId, false);
    }

    private ResponseAction handleOtherResponse(SessionId sessionId) {
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
