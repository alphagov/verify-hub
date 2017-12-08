package uk.gov.ida.hub.samlproxy.resources;

import com.codahale.metrics.annotation.Timed;
import javax.inject.Inject;

import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.controllogic.SamlMessageSenderHandler;
import uk.gov.ida.common.SessionId;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;
import static uk.gov.ida.hub.samlproxy.Urls.SharedUrls.SESSION_ID_PARAM;

@Path(Urls.SamlProxyUrls.SAML2_SSO_SENDER_API_ROOT)
@Produces(MediaType.APPLICATION_JSON)
public class SamlMessageSenderApi {

    private final SamlMessageSenderHandler samlMessageSenderHandler;

    @Inject
    public SamlMessageSenderApi(final SamlMessageSenderHandler samlMessageSenderHandler){
        this.samlMessageSenderHandler = samlMessageSenderHandler;
    }

    @GET
    @Path(Urls.SamlProxyUrls.SEND_AUTHN_REQUEST_PATH)
    @Timed
    public Response sendJsonAuthnRequestFromHub(
            @QueryParam(SESSION_ID_PARAM) final SessionId sessionId,
            @HeaderParam(X_FORWARDED_FOR) String xForwardedFor) {
        return Response.ok(samlMessageSenderHandler.generateAuthnRequestFromHub(sessionId, xForwardedFor)).build();
    }

    @GET
    @Path(Urls.SamlProxyUrls.SEND_RESPONSE_FROM_HUB_PATH)
    @Timed
    public Response sendJsonAuthnResponseFromHub(
            @QueryParam(SESSION_ID_PARAM) final SessionId sessionId,
            @HeaderParam(X_FORWARDED_FOR) String xForwardedFor) {
        return Response.ok(samlMessageSenderHandler.generateAuthnResponseFromHub(sessionId, xForwardedFor)).build();
    }

    @GET
    @Path(Urls.SamlProxyUrls.SEND_ERROR_RESPONSE_FROM_HUB_PATH)
    @Timed
    public Response sendJsonErrorResponseFromHub(
            @QueryParam(SESSION_ID_PARAM) final SessionId sessionId,
            @HeaderParam(X_FORWARDED_FOR) String xForwardedFor) {
        return Response.ok(samlMessageSenderHandler.generateErrorResponseFromHub(sessionId, xForwardedFor)).build();
    }
}
