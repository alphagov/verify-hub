package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import httpstub.AbstractHttpStub;
import httpstub.HttpStub;
import httpstub.HttpStubRule;
import httpstub.RegisteredResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

import static httpstub.builders.RegisteredResponseBuilder.aRegisteredResponse;

public class MsaStubRule extends HttpStubRule {

    public static final String ATTRIBUTE_QUERY_RESOURCE = "/attribute-query-request";

    private MsaStubRule(AbstractHttpStub abstractHttpStub) {
        super(abstractHttpStub);
    }

    public static MsaStubRule sleepyMsaStubRule(final long sleepTime) {
        return new MsaStubRule(new SleepyHttpStub(sleepTime));
    }

    public static MsaStubRule msaStubRule() {
        return new MsaStubRule(new HttpStub());
    }

    public void prepareForAttributeQueryRequest(String response) throws JsonProcessingException {
        register(ATTRIBUTE_QUERY_RESOURCE, Response.Status.OK.getStatusCode(), response);
    }

    public void prepareForHealthCheckRequest(String response, String msaVersion) {
        RegisteredResponse registeredResponse = aRegisteredResponse()
                .withStatus(Response.Status.OK.getStatusCode())
                .withContentType(MediaType.TEXT_XML_TYPE.toString())
                .withBody(response)
                .withHeaders(ImmutableMap.of("ida-msa-version", msaVersion))
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
