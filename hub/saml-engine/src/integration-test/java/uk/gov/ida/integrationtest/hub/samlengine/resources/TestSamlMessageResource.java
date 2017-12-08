package uk.gov.ida.integrationtest.hub.samlengine.resources;

import com.codahale.metrics.annotation.Timed;
import org.joda.time.DateTime;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.contracts.ResponseFromHubDto;
import uk.gov.ida.hub.samlengine.factories.OutboundResponseFromHubToResponseTransformerFactory;
import uk.gov.ida.saml.core.domain.OutboundResponseFromHub;
import uk.gov.ida.saml.core.domain.TransactionIdaStatus;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;

@Path(Urls.SamlEngineUrls.SAML_ENGINE_ROOT + "/test")
public class TestSamlMessageResource {

    private OutboundResponseFromHubToResponseTransformerFactory transformerFactory;

    @Inject
    public TestSamlMessageResource(OutboundResponseFromHubToResponseTransformerFactory transformerFactory){
        this.transformerFactory = transformerFactory;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response generateRpAuthnResponseSamlMessage(ResponseFromHubDto responseFromHubDto, @QueryParam("transactionIdaStatus") TransactionIdaStatus transactionIdaStatus) {
        OutboundResponseFromHub authnResponseFromHub = new OutboundResponseFromHub(
                responseFromHubDto.getResponseId(),
                responseFromHubDto.getInResponseTo(),
                HUB_ENTITY_ID,
                DateTime.now(),
                transactionIdaStatus,
                responseFromHubDto.getMatchingServiceAssertion(),
                responseFromHubDto.getAssertionConsumerServiceUri());

        String samlResponseMessage = transformerFactory.get(responseFromHubDto.getAuthnRequestIssuerEntityId()).apply(authnResponseFromHub);
        return Response.ok(samlResponseMessage).build();
    }
}
