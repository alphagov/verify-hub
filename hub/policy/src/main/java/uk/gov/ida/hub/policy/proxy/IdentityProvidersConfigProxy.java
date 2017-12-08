package uk.gov.ida.hub.policy.proxy;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.annotations.Config;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;

@Singleton
public class IdentityProvidersConfigProxy {

    private final JsonClient jsonClient;
    private final URI configUri;

    @Inject
    public IdentityProvidersConfigProxy(
            JsonClient jsonClient,
            @Config URI configUri) {

        this.jsonClient = jsonClient;
        this.configUri = configUri;
    }

    @Timed
    public List<String> getEnabledIdentityProviders(Optional<String> transactionEntityId) {
        final UriBuilder uriBuilder = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.ENABLED_IDENTITY_PROVIDERS_RESOURCE);
        if (transactionEntityId.isPresent()) {
            uriBuilder.queryParam(Urls.ConfigUrls.TRANSACTION_ENTITY_ID_PARAM, transactionEntityId.get());
        }
        URI uri = uriBuilder.build();
        return jsonClient.get(uri, new GenericType<List<String>>() {
        });
    }

    @Timed
    public IdpConfigDto getIdpConfig(String identityProviderEntityId) {
        URI uri = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.IDENTITY_PROVIDER_CONFIG_DATA_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(identityProviderEntityId));

        return jsonClient.get(uri, IdpConfigDto.class);
    }

}
