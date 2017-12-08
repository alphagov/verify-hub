package uk.gov.ida.hub.samlsoapproxy.proxy;

import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.annotations.SamlEngine;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerRequestDto;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceHealthCheckerResponseDto;
import uk.gov.ida.hub.samlsoapproxy.contract.SamlMessageDto;
import uk.gov.ida.jerseyclient.JsonClient;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class SamlEngineProxy {

    private final JsonClient jsonClient;
    private final URI samlEngineUri;

    @Inject
    public SamlEngineProxy(JsonClient jsonClient, @SamlEngine URI samlEngineUri) {
        this.jsonClient = jsonClient;
        this.samlEngineUri = samlEngineUri;
    }

    public SamlMessageDto generateHealthcheckAttributeQuery(MatchingServiceHealthCheckerRequestDto matchingServiceHealthCheckerRequestDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.GENERATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESOURCE)
                .build();

        return jsonClient.post(matchingServiceHealthCheckerRequestDto, uri, SamlMessageDto.class);
    }

    public MatchingServiceHealthCheckerResponseDto translateHealthcheckMatchingServiceResponse(SamlMessageDto samlMessageDto) {
        URI uri = UriBuilder
                .fromUri(samlEngineUri)
                .path(Urls.SamlEngineUrls.TRANSLATE_MSA_HEALTHCHECK_ATTRIBUTE_QUERY_RESPONSE_RESOURCE)
                .build();
        return jsonClient.post(samlMessageDto, uri, MatchingServiceHealthCheckerResponseDto.class);
    }

}
