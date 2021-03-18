package uk.gov.ida.integrationtest.hub.samlproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.HttpStubRule;
import httpstub.RegisteredResponse;
import httpstub.builders.RegisteredResponseBuilder;
import uk.gov.ida.common.ErrorStatusDto;
import uk.gov.ida.common.ExceptionType;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlproxy.Urls;
import uk.gov.ida.hub.samlproxy.contracts.AuthnResponseFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.domain.AuthnRequestFromHubContainerDto;
import uk.gov.ida.hub.samlproxy.domain.LevelOfAssurance;
import uk.gov.ida.hub.samlproxy.domain.ResponseActionDto;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static uk.gov.ida.hub.samlproxy.Urls.PolicyUrls.NEW_SESSION_RESOURCE;
import static uk.gov.ida.hub.samlproxy.Urls.PolicyUrls.SESSION_RESOURCE_ROOT;
import static uk.gov.ida.hub.samlproxy.Urls.SharedUrls.SESSION_ID_PARAM_PATH;

public class PolicyStubRule extends HttpStubRule {

    public void stubCreateSession(SessionId sessionId) throws JsonProcessingException {
        URI locationUri = baseUri().path(SESSION_RESOURCE_ROOT + SESSION_ID_PARAM_PATH).build(sessionId.getSessionId());
        RegisteredResponse responseFromPolicy = RegisteredResponseBuilder.aRegisteredResponse()
                .withStatus(Status.CREATED.getStatusCode())
                .withHeaders(Map.of(HttpHeaders.LOCATION, locationUri.toASCIIString()))
                .withBody(sessionId)
                .build();
        register(NEW_SESSION_RESOURCE, responseFromPolicy);
    }

    public void returnErrorForCreateSession() throws JsonProcessingException {
        register(NEW_SESSION_RESOURCE, INTERNAL_SERVER_ERROR.getStatusCode(), ErrorStatusDto.createAuditedErrorStatus(UUID.randomUUID(), ExceptionType.NETWORK_ERROR));
    }

    public void receiveAuthnResponseFromIdp(String sessionId, LevelOfAssurance loaAchieved) throws JsonProcessingException {
        String locationUri = getAuthnResponseFromIdpLocation(sessionId);
        ResponseActionDto responseActionDto = ResponseActionDto.success(new SessionId(sessionId), false, loaAchieved, null);
        register(locationUri, Status.OK.getStatusCode(), responseActionDto);
    }

    private String getAuthnResponseFromIdpLocation(String sessionId) {
        return UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_RESPONSE_RESOURCE).build(sessionId).toASCIIString();
    }

    public void receiveAuthnResponseFromIdpError(String sessionId) {
        register(getAuthnResponseFromIdpLocation(sessionId), Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    public void aValidAuthnRequestFromHubToIdp(SessionId sessionId, AuthnRequestFromHubContainerDto body) throws JsonProcessingException {
        register(
                UriBuilder.fromPath(Urls.PolicyUrls.IDP_AUTHN_REQUEST_RESOURCE).build(sessionId).getPath(),
                RegisteredResponseBuilder
                        .aRegisteredResponse()
                        .withBody(body)
                        .build()
        );
    }

    public void anAuthnResponseFromHubToRp(SessionId sessionId, AuthnResponseFromHubContainerDto body) throws JsonProcessingException {
        register(
                UriBuilder.fromPath(Urls.PolicyUrls.RP_AUTHN_RESPONSE_RESOURCE).build(sessionId).getPath(),
                RegisteredResponseBuilder
                        .aRegisteredResponse()
                        .withBody(body)
                        .build()
        );
    }

    public void anErrorResponseFromHubToRp(SessionId sessionId, AuthnResponseFromHubContainerDto body) throws JsonProcessingException {
        register(
                UriBuilder.fromPath(Urls.PolicyUrls.RP_ERROR_RESPONSE_RESOURCE).build(sessionId).getPath(),
                RegisteredResponseBuilder
                        .aRegisteredResponse()
                        .withBody(body)
                        .build()
        );
    }

}
