package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConnectedServiceConfig {

    @JsonProperty("entity_id")
    protected String entityId;

    @JsonProperty("service_provider_id")
    protected String serviceProviderConfigId;

    private RemoteServiceProviderConfig serviceProviderConfig;

    @SuppressWarnings("unused")
    protected RemoteConnectedServiceConfig() {
    }

    public String getEntityId() {
        return entityId;
    }

    public String getServiceProviderConfigId() {
        return serviceProviderConfigId;
    }

    public RemoteConnectedServiceConfig withServiceProviderConfig(RemoteServiceProviderConfig serviceProviderConfig) {
        this.serviceProviderConfig = serviceProviderConfig;
        return this;
    }

    public RemoteServiceProviderConfig getServiceProviderConfig() {
        return serviceProviderConfig;
    }
}
