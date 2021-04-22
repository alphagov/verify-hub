package uk.gov.ida.hub.samlsoapproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.configuration.JerseyClientWithRetryBackoffConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.hub.samlsoapproxy.config.PrometheusClientServiceConfiguration;
import uk.gov.ida.hub.samlsoapproxy.config.SamlConfiguration;
import uk.gov.ida.metrics.config.PrometheusConfiguration;
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
public class SamlSoapProxyConfiguration extends Configuration implements RestfulClientConfiguration, TrustStoreConfiguration, ServiceNameConfiguration, PrometheusConfiguration {

    @Valid
    @NotNull
    @JsonProperty
    protected SamlConfiguration saml;

    @Valid
    @JsonProperty
    @NotNull
    protected ExecutorConfiguration matchingServiceExecutorConfiguration;

    @Valid
    @NotNull
    @JsonProperty
    protected JerseyClientWithRetryBackoffConfiguration soapHttpClient;

    @Valid
    @NotNull
    @JsonProperty
    protected JerseyClientConfiguration healthCheckSoapHttpClient;

    @Valid
    @NotNull
    @JsonProperty
    protected URI policyUri;

    @Valid
    @NotNull
    @JsonProperty
    protected MultiTrustStoresBackedMetadataConfiguration metadata;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean enableRetryTimeOutConnections = false;

    @Valid
    @JsonProperty
    protected URI eventSinkUri;

    @Valid
    @NotNull
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
    @JsonProperty
    protected URI configUri;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration certificatesConfigCacheExpiry = Duration.minutes(5);

    @Valid
    @NotNull
    @JsonProperty
    protected ClientTrustStoreConfiguration rpTrustStoreConfiguration;

    @Valid
    @NotNull
    @JsonProperty
    public EventEmitterConfiguration eventEmitterConfiguration;

    @Valid
    @JsonProperty
    private PrometheusClientServiceConfiguration matchingServiceHealthCheckServiceConfiguration = new PrometheusClientServiceConfiguration();

    protected SamlSoapProxyConfiguration() {
    }

    public SamlConfiguration getSamlConfiguration() {
        return saml;
    }

    public ExecutorConfiguration getMatchingServiceExecutor() {
        return matchingServiceExecutorConfiguration;
    }

    public JerseyClientWithRetryBackoffConfiguration getSoapJerseyClientConfiguration() {
        return soapHttpClient;
    }

    public JerseyClientConfiguration getHealthCheckSoapHttpClient() {
        return healthCheckSoapHttpClient;
    }

    public URI getPolicyUri() {
        return policyUri;
    }

    public Optional<MetadataResolverConfiguration> getMetadataConfiguration() {
        return Optional.of(metadata);
    }

    @Override
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return httpClient;
    }

    public URI getEventSinkUri() {
        return eventSinkUri;
    }

    public URI getSamlEngineUri() {
        return samlEngineUri;
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

    public EventEmitterConfiguration getEventEmitterConfiguration() {
        return eventEmitterConfiguration;
    }

    @Override
    public boolean isPrometheusEnabled() {
        return true;
    }

    public PrometheusClientServiceConfiguration getMatchingServiceHealthCheckServiceConfiguration() {
        return matchingServiceHealthCheckServiceConfiguration;
    }
}
