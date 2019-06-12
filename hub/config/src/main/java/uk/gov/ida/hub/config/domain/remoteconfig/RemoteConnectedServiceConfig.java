package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConnectedServiceConfig {

    @JsonProperty("entity_id")
    protected String entityId;

    @JsonProperty("service_provider_id")
    protected int serviceProviderConfigId;

    @SuppressWarnings("unused")
    protected RemoteConnectedServiceConfig() {
    }

    public String getEntityId() {
        return entityId;
    }

    public int getServiceProviderConfigId() {
        return serviceProviderConfigId;
    }

}
