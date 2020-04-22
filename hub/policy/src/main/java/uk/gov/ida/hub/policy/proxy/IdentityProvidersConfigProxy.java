package uk.gov.ida.hub.policy.proxy;

import com.codahale.metrics.annotation.Timed;
import uk.gov.ida.hub.policy.Urls;
import uk.gov.ida.hub.policy.annotations.Config;
import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.jerseyclient.JsonClient;
import uk.gov.ida.shared.utils.string.StringEncoding;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collections;
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
    public List<String> getEnabledIdentityProvidersForAuthenticationRequestGeneration(String transactionEntityId, boolean registering, LevelOfAssurance loa) {
        return getEnabledIdentityProviders(transactionEntityId, loa, registering, false);
    }

    @Timed
    public List<String> getEnabledIdentityProvidersForAuthenticationResponseProcessing(String transactionEntityId, boolean registering, LevelOfAssurance loa) {
        return getEnabledIdentityProviders(transactionEntityId, loa, registering, true);
    }

    private List<String> getEnabledIdentityProviders(String transactionEntityId, LevelOfAssurance loa, boolean registering, boolean processingIdpResponse) {
        if (transactionEntityId == null) {
            return Collections.emptyList();
        }

        return registering ?
                getEnabledIdentityProvidersForRegistration(transactionEntityId, loa, processingIdpResponse) :
                getEnabledIdentityProvidersForSignIn(transactionEntityId);
    }

    @Timed
    public IdpConfigDto getIdpConfig(String identityProviderEntityId) {
        URI uri = UriBuilder
                .fromUri(configUri)
                .path(Urls.ConfigUrls.IDENTITY_PROVIDER_CONFIG_DATA_RESOURCE)
                .buildFromEncoded(StringEncoding.urlEncode(identityProviderEntityId));

        return jsonClient.get(uri, IdpConfigDto.class);
    }

    @Timed
    public boolean isIDPEnabledForRegistration(String idpEntityID, String transactionEntityId, LevelOfAssurance levelOfAssurance) {
        List<String> enabledIdentityProvidersForRegistration = getEnabledIdentityProvidersForRegistration(transactionEntityId, levelOfAssurance, false);

        return enabledIdentityProvidersForRegistration.contains(idpEntityID);
    }

    private List<String> getEnabledIdentityProvidersForRegistration(String transactionEntityId, LevelOfAssurance levelOfAssurance, boolean processingIdpResponse) {
        final String enabledIdpConfigServiceResourceUrl = processingIdpResponse ?
                Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_RESPONSE_RESOURCE :
                Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_REGISTRATION_AUTHN_REQUEST_RESOURCE;

        final UriBuilder uriBuilder = UriBuilder.fromUri(configUri).path(enabledIdpConfigServiceResourceUrl);
        final URI uri = uriBuilder.buildFromEncoded(StringEncoding.urlEncode(transactionEntityId), levelOfAssurance.toString());
        return jsonClient.get(uri, new GenericType<List<String>>() {});
    }

    private List<String> getEnabledIdentityProvidersForSignIn(String transactionEntityId) {
        final UriBuilder uriBuilder = UriBuilder.fromUri(configUri).path(Urls.ConfigUrls.ENABLED_ID_PROVIDERS_FOR_SIGN_IN_RESOURCE);
        final URI uri = uriBuilder.buildFromEncoded(StringEncoding.urlEncode(transactionEntityId));
        return jsonClient.get(uri, new GenericType<List<String>>() {});
    }
}
