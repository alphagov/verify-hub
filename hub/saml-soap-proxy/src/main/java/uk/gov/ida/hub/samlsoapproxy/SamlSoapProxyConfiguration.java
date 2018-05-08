package uk.gov.ida.hub.samlsoapproxy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.hub.samlsoapproxy.config.SamlConfiguration;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.saml.metadata.MetadataResolverConfiguration;
import uk.gov.ida.saml.metadata.MultiTrustStoresBackedMetadataConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.TrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlSoapProxyConfiguration extends Configuration implements RestfulClientConfiguration, TrustStoreConfiguration, ServiceNameConfiguration {

    protected SamlSoapProxyConfiguration() {
    }

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
    protected JerseyClientConfiguration soapHttpClient;

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
    @NotNull
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
    protected ClientTrustStoreConfiguration rpTrustStoreConfiguration;

    @Valid
    @JsonProperty
    public EventEmitterConfiguration eventEmitterConfiguration;

    public SamlConfiguration getSamlConfiguration() {
        return saml;
    }

    public ExecutorConfiguration getMatchingServiceExecutor() {
        return matchingServiceExecutorConfiguration;
    }

    public JerseyClientConfiguration getSoapJerseyClientConfiguration() {
        return soapHttpClient;
    }

    public JerseyClientConfiguration getHealthCheckSoapHttpClient() {
        return healthCheckSoapHttpClient;
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

    public URI getSamlEngineUri() {
        return samlEngineUri;
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

    public EventEmitterConfiguration getEventEmitterConfiguration() {
        return eventEmitterConfiguration;
    }
}
