package uk.gov.ida.hub.samlproxy;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.hub.samlproxy.config.CountryConfiguration;
import uk.gov.ida.hub.samlproxy.config.SamlConfiguration;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.MultiTrustStoresBackedMetadataConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlProxyConfiguration extends Configuration implements RestfulClientConfiguration, TrustStoreConfiguration, ServiceNameConfiguration {

    protected SamlProxyConfiguration(){}

    @Valid
    @NotNull
    @JsonProperty
    protected SamlConfiguration saml;

    @Valid
    @NotNull
    @JsonProperty
    protected URI frontendExternalUri;

    @NotNull
    @Valid
    @JsonProperty
    protected Duration metadataValidDuration;
    
    @NotNull
    @Valid
    @JsonProperty
    protected MultiTrustStoresBackedMetadataConfiguration metadata;

    @Valid
    @NotNull
    @JsonProperty
    protected URI policyUri;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean enableRetryTimeOutConnections = false;

    @Valid
    @NotNull
    @JsonProperty
    protected URI eventSinkUri;

    @Deprecated
    @Valid
    @JsonProperty
    protected URI samlEngineUri;

    @Valid
    @NotNull
    @JsonProperty
    protected JerseyClientConfiguration httpClient;

    @JsonProperty
    @NotNull
    @Valid
    protected ServiceInfoConfiguration serviceInfo;

    @Valid
    @NotNull
    @JsonProperty
    protected URI configUri;

    @Valid
    @NotNull
    @JsonProperty
    protected ClientTrustStoreConfiguration rpTrustStoreConfiguration;

    @Valid
    @JsonProperty
    protected CountryConfiguration country;

    @Valid
    @JsonProperty
    public EventEmitterConfiguration eventEmitterConfiguration;

    public SamlConfiguration getSamlConfiguration() {
        return saml;
    }

    public Duration getMetadataValidDuration() {
        return metadataValidDuration;
    }

    public URI getFrontendExternalUri() {
        return frontendExternalUri;
    }

    public URI getPolicyUri() {
        return policyUri;
    }

    public MetadataResolverConfiguration getMetadataConfiguration() {
       return metadata;
    }

    @Override
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return httpClient;
    }

    public URI getEventSinkUri() {
        return eventSinkUri;
    }

    public URI getConfigUri() {
        return configUri;
    }

    public ServiceInfoConfiguration getServiceInfo() {
        return serviceInfo;
    }

    @Override
    public String getServiceName() {
        return serviceInfo.getName();
    }

    @Override
    public ClientTrustStoreConfiguration getRpTrustStoreConfiguration() {
        return this.rpTrustStoreConfiguration;
    }

    @Override
    public boolean getEnableRetryTimeOutConnections() { return enableRetryTimeOutConnections; }

    public Optional<CountryConfiguration> getCountryConfiguration() {
        return country != null ? Optional.of(country) : Optional.empty();
    }

    public EventEmitterConfiguration getEventEmitterConfiguration() {
        return eventEmitterConfiguration;
    }

    public boolean isEidasEnabled(){
        return getCountryConfiguration().isPresent();
    }
}
