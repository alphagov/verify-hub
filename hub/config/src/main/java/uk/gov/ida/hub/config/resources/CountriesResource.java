package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.CountryConfig;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

@Path(Urls.ConfigUrls.COUNTRIES_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class CountriesResource {

    private final LocalConfigRepository<CountryConfig> countryConfigRepository;

    @Inject
    public CountriesResource(
            LocalConfigRepository<CountryConfig> countryConfigRepository) {
        this.countryConfigRepository = countryConfigRepository;
    }

    @GET
    @Timed
    public Collection<CountryConfig> getEidasCountries() {
        return countryConfigRepository.getAllData();
    }
}
