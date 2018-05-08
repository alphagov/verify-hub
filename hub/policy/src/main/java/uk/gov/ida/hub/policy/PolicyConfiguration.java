package uk.gov.ida.hub.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.util.Duration;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.restclient.RestfulClientConfiguration;
import uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanConfiguration;
import uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanServiceConfiguration;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicyConfiguration extends Configuration implements RestfulClientConfiguration, ServiceNameConfiguration, InfinispanServiceConfiguration, AssertionLifetimeConfiguration {

    @Valid
    @JsonProperty
    @NotNull
    public JerseyClientConfiguration samlSoapProxyClient;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration timeoutPeriod;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration matchingServiceResponseWaitPeriod;

    @Valid
    @NotNull
    @JsonProperty
    protected InfinispanConfiguration infinispan;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration assertionLifetime;

    @Valid
    @NotNull
    @JsonProperty
    protected URI samlSoapProxyUri;

    @Valid
    @NotNull
    @JsonProperty
    public Boolean enableRetryTimeOutConnections = false;

    @Valid
    @NotNull
    @JsonProperty
    public URI eventSinkUri;

    @Valid
    @NotNull
    @JsonProperty
    public URI samlEngineUri;

    @Valid
    @NotNull
    @JsonProperty
    public JerseyClientConfiguration httpClient;

    @JsonProperty
    @NotNull
    @Valid
    public ServiceInfoConfiguration serviceInfo;

    @Valid
    @NotNull
    @JsonProperty
    public URI configUri;

    @Valid
    @NotNull
    @JsonProperty
    public ClientTrustStoreConfiguration clientTrustStoreConfiguration;

    @JsonProperty
    public Boolean eidas = false;

    @Valid
    @JsonProperty
    public EventEmitterConfiguration eventEmitterConfiguration;

    protected PolicyConfiguration() {}

    public URI getSamlSoapProxyUri() { return samlSoapProxyUri;  }

    public org.joda.time.Duration getSessionLength() {
        return new org.joda.time.Duration(timeoutPeriod.toMilliseconds());
    }

    public org.joda.time.Duration getMatchingServiceResponseWaitPeriod(){
        return new org.joda.time.Duration(matchingServiceResponseWaitPeriod.toMilliseconds());
    }

    @Override
    public InfinispanConfiguration getInfinispan() {
        return infinispan;
    }

    @Override
    public Duration getAssertionLifetime() {
        return assertionLifetime;
    }

    public JerseyClientConfiguration getSamlSoapProxyClient() {
        return samlSoapProxyClient;
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

    public ClientTrustStoreConfiguration getClientTrustStoreConfiguration() {
        return this.clientTrustStoreConfiguration;
    }

    @Override
    public boolean getEnableRetryTimeOutConnections() { return enableRetryTimeOutConnections; }

    public boolean isEidasEnabled() {
        return eidas;
    }

    public EventEmitterConfiguration getEventEmitterConfiguration() {
        return eventEmitterConfiguration;
    }
}
