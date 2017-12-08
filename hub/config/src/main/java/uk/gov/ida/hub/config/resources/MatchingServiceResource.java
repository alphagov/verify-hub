package uk.gov.ida.hub.config.resources;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.config.Urls;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.MatchingServiceConfigEntityDataDto;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;

@Path(Urls.ConfigUrls.MATCHING_SERVICE_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class MatchingServiceResource {
    private final ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository;
    private final ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository;

    @Inject
    public MatchingServiceResource(
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionConfigEntityDataRepository,
            ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceConfigEntityDataRepository) {
        this.transactionConfigEntityDataRepository = transactionConfigEntityDataRepository;
        this.matchingServiceConfigEntityDataRepository = matchingServiceConfigEntityDataRepository;
    }

    @GET
    @Path(Urls.ConfigUrls.MATCHING_SERVICE_PATH)
    @Timed
    public MatchingServiceConfigEntityDataDto getMatchingService(
            @PathParam(Urls.SharedUrls.ENTITY_ID_PARAM) String entityId) {
        MatchingServiceConfigEntityData matchingServiceConfigEntityData = matchingServiceConfigEntityDataRepository.getData(entityId).get();
        return new MatchingServiceConfigEntityDataDto(
                matchingServiceConfigEntityData.getEntityId(),
                matchingServiceConfigEntityData.getUri(),
                entityId,
                matchingServiceConfigEntityData.getHealthCheckEnabled(),
                matchingServiceConfigEntityData.getOnboarding(),
                matchingServiceConfigEntityData.getUserAccountCreationUri());
    }

    @GET
    @Timed
    public Collection<MatchingServiceConfigEntityDataDto> getMatchingServices() {
        Collection<MatchingServiceConfigEntityDataDto> matchingServices = new ArrayList<>();
        for (TransactionConfigEntityData transactionConfigEntityData : transactionConfigEntityDataRepository.getAllData()) {

            MatchingServiceConfigEntityData matchingServiceConfigEntityData = matchingServiceConfigEntityDataRepository.getData(transactionConfigEntityData.getMatchingServiceEntityId()).get();
            matchingServices.add(new MatchingServiceConfigEntityDataDto(
                    matchingServiceConfigEntityData.getEntityId(),
                    matchingServiceConfigEntityData.getUri(),
                    transactionConfigEntityData.getEntityId(),
                    matchingServiceConfigEntityData.getHealthCheckEnabled(),
                    matchingServiceConfigEntityData.getOnboarding(),
                    matchingServiceConfigEntityData.getUserAccountCreationUri()));
        }
        return matchingServices;
    }
}
