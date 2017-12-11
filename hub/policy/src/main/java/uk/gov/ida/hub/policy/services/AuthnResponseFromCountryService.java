package uk.gov.ida.hub.policy.services;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.contracts.AttributeQueryContainerDto;
import uk.gov.ida.hub.policy.contracts.EidasAttributeQueryRequestDto;
import uk.gov.ida.hub.policy.contracts.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.hub.policy.domain.AssertionRestrictionsFactory;
import uk.gov.ida.hub.policy.domain.IdpIdaStatus;
import uk.gov.ida.hub.policy.domain.InboundResponseFromCountry;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.PersistentId;
import uk.gov.ida.hub.policy.domain.ResponseAction;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.SessionRepository;
import uk.gov.ida.hub.policy.domain.controller.CountrySelectedStateController;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.factories.SamlAuthnResponseTranslatorDtoFactory;
import uk.gov.ida.hub.policy.proxy.AttributeQueryRequest;
import uk.gov.ida.hub.policy.proxy.MatchingServiceConfigProxy;
import uk.gov.ida.hub.policy.proxy.SamlEngineProxy;
import uk.gov.ida.hub.policy.proxy.SamlSoapProxyProxy;

import javax.inject.Inject;

public class AuthnResponseFromCountryService {

    private static Logger LOG = LoggerFactory.getLogger(AuthnResponseFromCountryService.class);

    private final SamlEngineProxy samlEngineProxy;
    private final SamlSoapProxyProxy samlSoapProxyProxy;
    private final SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory;
    private final CountriesService countriesService;
    private final SessionRepository sessionRepository;
    private final MatchingServiceConfigProxy matchingServiceConfigProxy;
    private final PolicyConfiguration policyConfiguration;
    private final AssertionRestrictionsFactory assertionRestrictionFactory;


    @Inject
    public AuthnResponseFromCountryService(SamlEngineProxy samlEngineProxy,
                                           SamlSoapProxyProxy samlSoapProxyProxy,
                                           MatchingServiceConfigProxy matchingServiceConfigProxy,
                                           PolicyConfiguration policyConfiguration,
                                           SessionRepository sessionRepository,
                                           SamlAuthnResponseTranslatorDtoFactory samlAuthnResponseTranslatorDtoFactory,
                                           CountriesService countriesService,
                                           AssertionRestrictionsFactory assertionRestrictionFactory) {
        this.samlEngineProxy = samlEngineProxy;
        this.samlSoapProxyProxy = samlSoapProxyProxy;
        this.matchingServiceConfigProxy = matchingServiceConfigProxy;
        this.policyConfiguration = policyConfiguration;
        this.sessionRepository = sessionRepository;
        this.samlAuthnResponseTranslatorDtoFactory = samlAuthnResponseTranslatorDtoFactory;
        this.countriesService = countriesService;
        this.assertionRestrictionFactory = assertionRestrictionFactory;
    }

    public ResponseAction receiveAuthnResponseFromCountry(SessionId sessionId,
                                                          SamlAuthnResponseContainerDto responseFromCountry) {

        CountrySelectedStateController stateController = (CountrySelectedStateController) sessionRepository.getStateController(sessionId, CountrySelectedState.class);
        String matchingServiceEntityId = stateController.getMatchingServiceEntityId();
        stateController.validateCountryIsIn(countriesService.getCountries(sessionId));

        SamlAuthnResponseTranslatorDto responseToTranslate = samlAuthnResponseTranslatorDtoFactory.fromSamlAuthnResponseContainerDto(responseFromCountry, matchingServiceEntityId);
        InboundResponseFromCountry translatedResponse = samlEngineProxy.translateAuthnResponseFromCountry(responseToTranslate);
        validateTranslatedResponse(stateController, translatedResponse);
        EidasAttributeQueryRequestDto eidasAttributeQueryRequestDto = getEidasAttributeQueryRequestDto(stateController, translatedResponse);
        stateController.transitionToEidasCycle0And1MatchRequestSentState(eidasAttributeQueryRequestDto, responseFromCountry.getPrincipalIPAddressAsSeenByHub(), translatedResponse.getIssuer());
        AttributeQueryContainerDto aqr = samlEngineProxy.generateEidasAttributeQuery(eidasAttributeQueryRequestDto);
        samlSoapProxyProxy.sendHubMatchingServiceRequest(sessionId, getAttributeQueryRequest(aqr));

        return ResponseAction.success(sessionId, false, LevelOfAssurance.LEVEL_2);
    }

    private AttributeQueryRequest getAttributeQueryRequest(AttributeQueryContainerDto aqr) {
        return new AttributeQueryRequest(aqr.getId(), aqr.getIssuer(), aqr.getSamlRequest(), aqr.getMatchingServiceUri(), aqr.getAttributeQueryClientTimeOut(), aqr.isOnboarding());
    }

    private void validateTranslatedResponse(CountrySelectedStateController controller, InboundResponseFromCountry dto) {
        controller.validateLevelOfAssurance(dto.getLevelOfAssurance().transform(java.util.Optional::of).or(java.util.Optional.empty()));
        if (dto.getStatus() != IdpIdaStatus.Status.Success) {
            throw StateProcessingValidationException.authnResponseTranslationFailed(controller.getRequestId(), dto.getStatus());
        }
        if (!dto.getPersistentId().isPresent()) {
            throw StateProcessingValidationException.missingMandatoryAttribute(controller.getRequestId(), "persistentId");
        }
        if (!dto.getEncryptedIdentityAssertionBlob().isPresent()) {
            throw StateProcessingValidationException.missingMandatoryAttribute(controller.getRequestId(), "encryptedIdentityAssertionBlob");
        }

        if (!dto.getLevelOfAssurance().isPresent()) {
            throw StateProcessingValidationException.missingMandatoryAttribute(controller.getRequestId(), "levelOfAssurance");
        }
    }

    private EidasAttributeQueryRequestDto getEidasAttributeQueryRequestDto(CountrySelectedStateController stateController, InboundResponseFromCountry response) {
        final String matchingServiceEntityId = stateController.getMatchingServiceEntityId();
        MatchingServiceConfigEntityDataDto matchingServiceConfig = matchingServiceConfigProxy.getMatchingService(matchingServiceEntityId);
        return new EidasAttributeQueryRequestDto(
            stateController.getRequestId(),
            stateController.getRequestIssuerEntityId(),
            stateController.getAssertionConsumerServiceUri(),
            assertionRestrictionFactory.getAssertionExpiry(),
            matchingServiceEntityId,
            matchingServiceConfig.getUri(),
            DateTime.now().plus(policyConfiguration.getMatchingServiceResponseWaitPeriod()),
            matchingServiceConfig.isOnboarding(),
            response.getLevelOfAssurance().get(),
            new PersistentId(response.getPersistentId().get()),
            Optional.absent(),
            Optional.absent(),
            response.getEncryptedIdentityAssertionBlob().get()
        );
    }
}
