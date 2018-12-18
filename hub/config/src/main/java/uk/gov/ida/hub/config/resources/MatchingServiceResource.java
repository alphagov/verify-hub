package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.application.MatchingServiceAdapterService;
import uk.gov.ida.hub.config.dto.MatchingServiceConfigEntityDataDto;

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
    private final MatchingServiceAdapterService matchingServiceAdapterService;

    @Inject
    public MatchingServiceResource(MatchingServiceAdapterService matchingServiceAdapterService) {
        this.matchingServiceAdapterService = matchingServiceAdapterService;
    }

    @GET
    @Path(Urls.ConfigUrls.MATCHING_SERVICE_PATH)
    @Timed
    public MatchingServiceConfigEntityDataDto getMatchingService(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        MatchingServiceAdapterService.MatchingServicePerTransaction matchingServicePerTransaction = matchingServiceAdapterService.getMatchingService(entityId);

        return toMatchingServiceDto(matchingServicePerTransaction);
    }

    @GET
    @Timed
    public Collection<MatchingServiceConfigEntityDataDto> getMatchingServices() {
        return matchingServiceAdapterService.getMatchingServices().stream()
                .map(this::toMatchingServiceDto)
                .collect(toList());
    }

    private MatchingServiceConfigEntityDataDto toMatchingServiceDto(MatchingServiceAdapterService.MatchingServicePerTransaction matchingServicePerTransaction) {
        return new MatchingServiceConfigEntityDataDto(
                matchingServicePerTransaction.getEntityId(),
                matchingServicePerTransaction.getUri(),
                matchingServicePerTransaction.getEntityId(),
                matchingServicePerTransaction.getHealthCheckEnabled(),
                matchingServicePerTransaction.isOnboarding(),
                matchingServicePerTransaction.getReadMetadataFromEntityId(),
                matchingServicePerTransaction.getUserAccountCreationUri());
    }
}
