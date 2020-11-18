package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.domain.CountryConfig;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.Collection;

import static uk.gov.ida.hub.config.Urls.ConfigUrls.EIDAS_DISABLED_AFTER_PATH;

@Path(Urls.ConfigUrls.COUNTRIES_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class CountriesResource {

    private final LocalConfigRepository<CountryConfig> countryConfigRepository;
    private final ConfigConfiguration configuration;

    @Inject
    public CountriesResource(
            LocalConfigRepository<CountryConfig> countryConfigRepository, ConfigConfiguration configuration) {
        this.countryConfigRepository = countryConfigRepository;
        this.configuration = configuration;
    }

    @GET
    @Timed
    public Collection<CountryConfig> getEidasCountries() {
        return countryConfigRepository.getAllData();
    }

    @Path(EIDAS_DISABLED_AFTER_PATH)
    @GET
    @Timed
    public LocalDateTime getEidasDisabledAfter() {
        return configuration.getEidasDisabledAfter().orElse(null);
    }
}
