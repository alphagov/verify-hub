package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.ResponseFromHub;
import uk.gov.ida.hub.policy.domain.ResponseFromHubFactory;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.exception.IdpDisabledException;
import uk.gov.ida.hub.policy.services.CountriesService;

import java.util.List;

public class EidasSuccessfulMatchStateController extends AbstractSuccessfulMatchStateController<EidasSuccessfulMatchState> {

    private CountriesService countriesService;

    public EidasSuccessfulMatchStateController(
            EidasSuccessfulMatchState state,
            ResponseFromHubFactory responseFromHubFactory,
            CountriesService countriesService) {

        super(state, responseFromHubFactory);

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
