package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConnectedServiceConfig {

    @JsonProperty("entity_id")
    protected String entityId;

    @JsonProperty("service_provider")
    protected RemoteServiceProviderConfig serviceProviderConfig;

    @JsonProperty("matching_service_adapter")
    protected RemoteMatchingServiceConfig matchingServiceConfig;

    @SuppressWarnings("unused")
    protected RemoteConnectedServiceConfig() {
    }

    public RemoteConnectedServiceConfig(String entityId, RemoteServiceProviderConfig serviceProviderConfig, RemoteMatchingServiceConfig matchingServiceConfig) {
        this.entityId = entityId;
        this.serviceProviderConfig = serviceProviderConfig;
        this.matchingServiceConfig = matchingServiceConfig;
    }

    public String getEntityId() {
        return entityId;
    }


    public RemoteServiceProviderConfig getServiceProviderConfig() {
        return serviceProviderConfig;
    }

    public RemoteMatchingServiceConfig getMatchingServiceConfig() {
        return matchingServiceConfig;
    }
}
