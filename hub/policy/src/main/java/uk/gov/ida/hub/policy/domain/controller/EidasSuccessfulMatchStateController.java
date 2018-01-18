package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.AbstractSuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.services.CountriesService;

import java.util.List;



public class EidasSuccessfulMatchStateController extends AbstractSuccessfulMatchStateController{

    private CountriesService countriesService;

    public EidasSuccessfulMatchStateController(AbstractSuccessfulMatchState state, ResponseFromHubFactory responseFromHubFactory, IdentityProvidersConfigProxy identityProvidersConfigProxy, CountriesService countriesService) {
        super(state, responseFromHubFactory, identityProvidersConfigProxy);
        this.countriesService = countriesService;
    }

    @Override
    public ResponseFromHub getPreparedResponse() {

        List<EidasCountryDto> enabledCountries = countriesService.getCountries(state.getSessionId());

        if (enabledCountries.stream().noneMatch(country -> country.getEntityId().equals(state.getIdentityProviderEntityId()))) {
            throw new IdpDisabledException(state.getIdentityProviderEntityId());
        }

        return getResponse();
    }
}
