package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import httpstub.AbstractHttpStub;
import httpstub.HttpStub;
import httpstub.HttpStubExtension;
import httpstub.RegisteredResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static httpstub.builders.RegisteredResponseBuilder.aRegisteredResponse;

public class MsaStubExtension extends HttpStubExtension {

    public static final String ATTRIBUTE_QUERY_RESOURCE = "/attribute-query-request";

    private MsaStubExtension(AbstractHttpStub abstractHttpStub) {
        super(abstractHttpStub);
    }

    public static MsaStubExtension sleepyMsaStubExtension(final long sleepTime) {
        return new MsaStubExtension(new SleepyHttpStub(sleepTime));
    }

    public static MsaStubExtension msaStubExtension() {
        return new MsaStubExtension(new HttpStub());
    }

    public void prepareForAttributeQueryRequest(String response) throws JsonProcessingException {
        register(ATTRIBUTE_QUERY_RESOURCE, Response.Status.OK.getStatusCode(), response);
    }

    public void prepareForHealthCheckRequest(String response) {
        RegisteredResponse registeredResponse = aRegisteredResponse()
                .withStatus(Response.Status.OK.getStatusCode())
                .withContentType(MediaType.TEXT_XML_TYPE.toString())
                .withBody(response)
                .build();
        register(ATTRIBUTE_QUERY_RESOURCE, registeredResponse);
    }

    public void respondWithBadHealthCheckResponse() throws JsonProcessingException {
        register(ATTRIBUTE_QUERY_RESOURCE, Response.Status.OK.getStatusCode(), "a-bad-response");
    }

    public URI getAttributeQueryRequestUri() {
        return uri(ATTRIBUTE_QUERY_RESOURCE);
    }

}
