package uk.gov.ida.hub.samlsoapproxy.resources;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.runnabletasks.AttributeQueryRequestRunnableFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;

@Produces(MediaType.APPLICATION_JSON)
@Path(Urls.SamlSoapProxyUrls.MATCHING_SERVICE_REQUEST_SENDER_RESOURCE)
public class AttributeQueryRequestSenderResource {
    private static final Logger LOG = LoggerFactory.getLogger(AttributeQueryRequestSenderResource.class);

    private final ExecutorService executorService;
    private final AttributeQueryRequestRunnableFactory runnableFactory;

    @Inject
    public AttributeQueryRequestSenderResource(
            AttributeQueryRequestRunnableFactory runnableFactory,
            ExecutorService executorService) {

        this.runnableFactory = runnableFactory;
        this.executorService = executorService;
    }

    @POST
    @Timed
    public Response sendAttributeQueryRequest(final AttributeQueryContainerDto attributeQueryContainerDto, @QueryParam(Urls.SharedUrls.SESSION_ID_PARAM) SessionId sessionId) {
        LOG.info("Received request to send attribute query {} to {}", attributeQueryContainerDto.getId(), attributeQueryContainerDto.getMatchingServiceUri());

        executorService.submit(runnableFactory.create(sessionId, attributeQueryContainerDto));

        LOG.info("Attribute query {} has been queued for sending to matching service.", attributeQueryContainerDto.getId());

        return Response.status(Response.Status.ACCEPTED).build();
    }
}
