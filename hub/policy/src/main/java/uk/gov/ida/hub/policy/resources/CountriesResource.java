package uk.gov.ida.hub.policy.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.services.CountriesService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static uk.gov.ida.hub.policy.Urls.PolicyUrls.COUNTRY_SET_PATH;
import static uk.gov.ida.hub.policy.Urls.PolicyUrls.COUNTRY_SET_PATH_PARAM;
import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM;
import static uk.gov.ida.hub.policy.Urls.SharedUrls.SESSION_ID_PARAM_PATH;

@Path(Urls.PolicyUrls.COUNTRIES_RESOURCE)
@Produces(MediaType.APPLICATION_JSON)
public class CountriesResource {
    private final CountriesService countriesService;

    @Inject
    public CountriesResource(CountriesService countriesService) {
        this.countriesService = countriesService;
    }

    @GET
    @Path(SESSION_ID_PARAM_PATH)
    @Timed
    public List<EidasCountryDto> getCountries(@PathParam(SESSION_ID_PARAM) SessionId sessionIdParameter) {
        return countriesService.getCountries(sessionIdParameter);
    }

    @POST
    @Path(COUNTRY_SET_PATH)
    @Timed
    public void setSelectedCountry(@PathParam(SESSION_ID_PARAM) SessionId sessionIdParameter,
                                   @PathParam(COUNTRY_SET_PATH_PARAM) String countryCode) {
        countriesService.setSelectedCountry(sessionIdParameter, countryCode);
    }
}
