package uk.gov.ida.hub.samlengine.proxy;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Singleton;
import uk.gov.ida.hub.samlengine.Urls;
import uk.gov.ida.hub.samlengine.annotations.Config;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class TransactionsConfigProxy {
    private final JsonClient jsonClient;
    private final URI configUri;

    @Inject
    public TransactionsConfigProxy(
            JsonClient jsonClient,
            @Config URI configUri) {

        this.jsonClient = jsonClient;
        this.configUri = configUri;
    }

    private LoadingCache<URI, Boolean> booleanCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<URI, Boolean>() {
                @Override
                public Boolean load(URI key) throws Exception {
                    return jsonClient.get(key, Boolean.class);
                }
            });

    public Boolean getShouldHubSignResponseMessages(String entityId) {
        final UriBuilder uriBuilder = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.SHOULD_HUB_SIGN_RESPONSE_MESSAGES_RESOURCE);
        for (Map.Entry<String, String> entry : ImmutableMap.<String, String>of().entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        URI uri = uriBuilder.buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        try {
            return booleanCache.getUnchecked(uri);
        } catch (UncheckedExecutionException e) {
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    public Boolean getShouldHubUseLegacySamlStandard(String entityId) {
        final UriBuilder uriBuilder = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.SHOULD_HUB_USE_LEGACY_SAML_STANDARD_RESOURCE);
        for (Map.Entry<String, String> entry : ImmutableMap.<String, String>of().entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
        URI uri = uriBuilder.buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
        try {
            return booleanCache.getUnchecked(uri);
        } catch (UncheckedExecutionException e) {
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }
    
    public Boolean getShouldSignWithSHA1(String entityId) {
        return true;
    }
}
