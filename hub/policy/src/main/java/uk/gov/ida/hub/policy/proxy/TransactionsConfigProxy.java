package uk.gov.ida.hub.policy.proxy;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.annotations.Config;
import uk.gov.ida.hub.policy.domain.EidasCountryDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.MatchingProcess;
import uk.gov.ida.hub.policy.domain.ResourceLocation;
import uk.gov.ida.hub.policy.domain.UserAccountCreationAttribute;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;

@Singleton
public class TransactionsConfigProxy {

    private final JsonClient jsonClient;
    private final URI configUri;

    private LoadingCache<URI, ResourceLocation> resourceLocation = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public ResourceLocation load(@Nonnull URI key) {
                    return jsonClient.get(key, ResourceLocation.class);
                }
            });

    @Inject
    public TransactionsConfigProxy(
            JsonClient jsonClient,
            @Config URI configUri) {

        this.jsonClient = jsonClient;
        this.configUri = configUri;
    }

    @Timed
    public ResourceLocation getAssertionConsumerServiceUri(String entityId, Optional<Integer> assertionConsumerServiceIndex) {

        Map<String, String> queryParams = emptyMap();

        if (assertionConsumerServiceIndex.isPresent()) {
            queryParams = Map.of(
                    Urls.ConfigUrls.ASSERTION_CONSUMER_SERVICE_INDEX_PARAM,
                    assertionConsumerServiceIndex.get().toString());
        }
        final URI uri = getEncodedUri(Urls.ConfigUrls.TRANSACTIONS_ASSERTION_CONSUMER_SERVICE_URI_RESOURCE, queryParams, entityId);
        try {
            return resourceLocation.getUnchecked(uri);
        } catch (UncheckedExecutionException e) {
            Throwables.throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }

    @Timed
    public MatchingProcess getMatchingProcess(String entityId) {
        return getConfigItem(
                entityId,
                Urls.ConfigUrls.MATCHING_PROCESS_RESOURCE,
                MatchingProcess.class,
                emptyMap());
    }

    @Timed
    public boolean getEidasSupportedForEntity(String entityId) {
        return getConfigItem(
            entityId,
            Urls.ConfigUrls.EIDAS_ENABLED_FOR_TRANSACTION_RESOURCE,
            boolean.class,
            emptyMap());
    }

    @Timed
    public boolean isUsingMatching( String entityId ) {
       return  getConfigItem(
                entityId,
                Urls.ConfigUrls.MATCHING_ENABLED_FOR_TRANSACTION_RESOURCE,
                boolean.class,
                emptyMap());
    }

    @Timed
    public List<LevelOfAssurance> getLevelsOfAssurance(String entityId) {
        final URI uriBuilder = getEncodedUri(Urls.ConfigUrls.LEVELS_OF_ASSURANCE_RESOURCE, emptyMap(), entityId);
        return jsonClient.get(uriBuilder, new GenericType<List<LevelOfAssurance>>() {});
    }

    @Timed
    public String getMatchingServiceEntityId(String entityId) {
        return getConfigItem(
                entityId,
                Urls.ConfigUrls.MATCHING_SERVICE_ENTITY_ID_RESOURCE,
                String.class,
                emptyMap());
    }

    @Timed
    public List<UserAccountCreationAttribute> getUserAccountCreationAttributes(String entityId) {
        final URI uri = getEncodedUri(Urls.ConfigUrls.USER_ACCOUNT_CREATION_ATTRIBUTES_RESOURCE, emptyMap(), entityId);

        return jsonClient.get(uri, new GenericType<List<UserAccountCreationAttribute>>() {
        });
    }

    private <T> T getConfigItem(String entityId, String path, Class<T> clazz, Map<String, String> queryParams) {
        final URI uriBuilder = getEncodedUri(path, queryParams, entityId);
        return jsonClient.get(uriBuilder, clazz);
    }

    private <T> T getConfigItem(String entityId, String path, GenericType<T> type, Map<String, String> queryParams) {
        final URI uriBuilder = getEncodedUri(path, queryParams, entityId);
        return jsonClient.get(uriBuilder, type);
    }

    private URI getEncodedUri(final String path, final Map<String, String> queryParams, final String entityId) {
        final UriBuilder uriBuilder = UriBuilder
                .fromUri(configUri)
                .path(path);
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }

        return uriBuilder.buildFromEncoded(StringEncoding.urlEncode(entityId).replace("+", "%20"));
    }

    @Timed
    public List<EidasCountryDto> getEidasSupportedCountries() {
        final URI uri = UriBuilder.fromUri(configUri).path(Urls.ConfigUrls.COUNTRIES_ROOT).build();
        return jsonClient.get(uri, new GenericType<List<EidasCountryDto>>() {});
    }

    @Timed
    public List<String> getEidasSupportedCountriesForRP(String relyingPartyEntityId) {
        return getConfigItem(
                relyingPartyEntityId,
                Urls.ConfigUrls.EIDAS_RP_COUNTRIES_FOR_TRANSACTION_RESOURCE,
                new GenericType<List<String>>() {
                },
                emptyMap());
    }

}
