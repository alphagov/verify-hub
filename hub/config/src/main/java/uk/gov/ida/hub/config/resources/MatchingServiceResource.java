package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.dto.MatchingServiceConfigDto;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

import static java.util.stream.Collectors.toList;

@Path(Urls.ConfigUrls.MATCHING_SERVICE_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class MatchingServiceResource {
    private final ManagedEntityConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;

    @Inject
    public MatchingServiceResource(ManagedEntityConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository) {
        this.matchingServiceConfigRepository = matchingServiceConfigRepository;
    }

    @GET
    @Path(Urls.ConfigUrls.MATCHING_SERVICE_PATH)
    @Timed
    public MatchingServiceConfigDto getMatchingService(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        MatchingServiceConfig config = matchingServiceConfigRepository.get(entityId).get();
        return toMatchingServiceDto(config);
    }

    @GET
    @Timed
    public Collection<MatchingServiceConfigDto> getMatchingServices() {
        return matchingServiceConfigRepository.getAll().stream()
                .map(this::toMatchingServiceDto)
                .collect(toList());
    }

    private MatchingServiceConfigDto toMatchingServiceDto(MatchingServiceConfig config) {
        return new MatchingServiceConfigDto(
                config.getEntityId(),
                config.getUri(),
                config.getEntityId(),
                config.getHealthCheckEnabled(),
                config.getOnboarding(),
                config.getUserAccountCreationUri());
    }
}
