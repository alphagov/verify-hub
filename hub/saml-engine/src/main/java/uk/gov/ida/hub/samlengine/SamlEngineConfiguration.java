package uk.gov.ida.hub.samlengine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import engineering.reliability.gds.metrics.config.PrometheusConfiguration;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.shared.configuration.PrivateKeyConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.hub.samlengine.config.RedisConfiguration;
import uk.gov.ida.hub.samlengine.config.SamlConfiguration;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.saml.hub.configuration.SamlAuthnRequestValidityDurationConfiguration;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.MultiTrustStoresBackedMetadataConfiguration;
import uk.gov.ida.shared.dropwizard.infinispan.config.CacheType;
import uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanConfiguration;
import uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanServiceConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;

import static com.google.common.base.Optional.absent;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlEngineConfiguration extends Configuration implements RestfulClientConfiguration, TrustStoreConfiguration, ServiceNameConfiguration, InfinispanServiceConfiguration, SamlDuplicateRequestValidationConfiguration, SamlAuthnRequestValidityDurationConfiguration, PrometheusConfiguration {

    protected SamlEngineConfiguration() {}

    @Valid
    @NotNull
    @JsonProperty
    protected SamlConfiguration saml;

    @Valid
    @JsonProperty
    protected PrivateKeyConfiguration privateSigningKeyConfiguration;

    @Valid
    @JsonProperty
    protected PrivateKeyConfiguration primaryPrivateEncryptionKeyConfiguration;

    @Valid
    @JsonProperty
    protected PrivateKeyConfiguration secondaryPrivateEncryptionKeyConfiguration;

    @Valid
    @JsonProperty
    protected InfinispanConfiguration infinispan = new InfinispanConfiguration(
            absent(), -1, absent(), absent(), CacheType.standalone, com.google.common.base.Optional.of(Duration.hours(2)),
            absent(), absent(), absent(), absent());

    @Valid
    @JsonProperty
    protected RedisConfiguration redis;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration authnRequestIdExpirationDuration;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration authnRequestValidityDuration;

    @NotNull
    @Valid
    @JsonProperty
    protected MultiTrustStoresBackedMetadataConfiguration metadata;

    @Valid
    @JsonProperty
    private CountryConfiguration country;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean enableRetryTimeOutConnections = false;

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

    //@Valid
    //@NotNull
    @JsonProperty
    protected Duration certificatesConfigCacheExpiry;

    @Valid
    @NotNull
    @JsonProperty
    protected ClientTrustStoreConfiguration rpTrustStoreConfiguration;

    public SamlConfiguration getSamlConfiguration() {
        return saml;
    }

    public Optional<CountryConfiguration> getCountryConfiguration() {
        return Optional.ofNullable(country);
    }

    public boolean isEidasEnabled() {
        return getCountryConfiguration().isPresent() && getCountryConfiguration().get().isEnabled();
    }

    public PrivateKeyConfiguration getPrivateSigningKeyConfiguration() {
        return privateSigningKeyConfiguration;
    }

    public PrivateKeyConfiguration getPrimaryPrivateEncryptionKeyConfiguration() {
        return primaryPrivateEncryptionKeyConfiguration;
    }

    public PrivateKeyConfiguration getSecondaryPrivateEncryptionKeyConfiguration() {
        return secondaryPrivateEncryptionKeyConfiguration;
    }

    @Override
    public InfinispanConfiguration getInfinispan() {
        return infinispan;
    }

    public Optional<RedisConfiguration> getRedis() {
        return Optional.ofNullable(redis);
    }

    @Override
    public Duration getAuthnRequestIdExpirationDuration() {
        return authnRequestIdExpirationDuration;
    }

    @Override
    public Duration getAuthnRequestValidityDuration() { return authnRequestValidityDuration; }


    public Optional<MetadataResolverConfiguration> getMetadataConfiguration() {
        return Optional.of(metadata);
    }

    @Override
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return httpClient;
    }

    public URI getConfigUri() {
        return configUri;
    }

    public Duration getCertificatesConfigCacheExpiry() {
        return certificatesConfigCacheExpiry;
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

    @Override
    public boolean isPrometheusEnabled() {
        return true;
    }
}
