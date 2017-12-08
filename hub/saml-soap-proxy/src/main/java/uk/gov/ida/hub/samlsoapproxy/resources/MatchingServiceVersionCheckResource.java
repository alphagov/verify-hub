package uk.gov.ida.hub.samlsoapproxy.resources;

import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.AggregatedMatchingServicesHealthCheckResult;
import uk.gov.ida.hub.samlsoapproxy.healthcheck.MatchingServiceHealthCheckHandler;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Path(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_VERSION_CHECK_RESOURCE)
public class MatchingServiceVersionCheckResource {

    private final MatchingServiceHealthCheckHandler matchingServiceHealthCheckHandler;

    @Inject
    public MatchingServiceVersionCheckResource(MatchingServiceHealthCheckHandler matchingServiceHealthCheckHandler) {
        this.matchingServiceHealthCheckHandler = matchingServiceHealthCheckHandler;
    }

    @GET
    public Response performMatchingServiceHealthCheck() {
        final AggregatedMatchingServicesHealthCheckResult result = matchingServiceHealthCheckHandler.forceCheckAllMSAs();

        final Response.ResponseBuilder response = result.isHealthy() ? Response.ok() : Response.serverError();
        response.entity(result);
        return response.build();
    }
}
