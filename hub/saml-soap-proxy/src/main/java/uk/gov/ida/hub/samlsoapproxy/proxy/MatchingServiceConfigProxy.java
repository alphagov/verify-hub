package uk.gov.ida.hub.samlsoapproxy.proxy;

import com.codahale.metrics.annotation.Timed;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Singleton;
import uk.gov.ida.hub.samlsoapproxy.Urls;
import uk.gov.ida.hub.samlsoapproxy.annotations.Config;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Singleton
public class MatchingServiceConfigProxy {

    private final JsonClient jsonClient;
    private final URI configUri;

    @Inject
    public MatchingServiceConfigProxy(
            JsonClient jsonClient,
            @Config URI configUri) {

        this.jsonClient = jsonClient;
        this.configUri = configUri;
    }

    private LoadingCache<URI, MatchingServiceConfigEntityDataDto> matchingServiceConfigEntityDataDto = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<URI, MatchingServiceConfigEntityDataDto>() {
                @Override
                public MatchingServiceConfigEntityDataDto load(URI key) {
                    return jsonClient.get(key, MatchingServiceConfigEntityDataDto.class);
                }
            });

    @Timed
    public MatchingServiceConfigEntityDataDto getMatchingService(String entityId) {
        final UriBuilder uriBuilder = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.MATCHING_SERVICE_RESOURCE);
        URI uri = uriBuilder.buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        try {
            return matchingServiceConfigEntityDataDto.getUnchecked(uri);
        } catch (UncheckedExecutionException e){
            throw e;
        }

    }

    @Timed
    public Collection<MatchingServiceConfigEntityDataDto> getMatchingServices() {
        UriBuilder uriBuilder = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.ENABLED_MATCHING_SERVICES_RESOURCE);
        URI uri = uriBuilder.build();
        return jsonClient.get(uri, new GenericType<Collection<MatchingServiceConfigEntityDataDto>>() {});
    }

}
