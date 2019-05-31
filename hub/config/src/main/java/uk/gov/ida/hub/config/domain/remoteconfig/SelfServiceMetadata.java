package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SelfServiceMetadata {

    protected SelfServiceMetadata() {}

    @JsonProperty("published_at")
    protected Date publishedAt;

    @JsonProperty("connected_services")
    protected List<RemoteConnectedServiceConfig> connectedServices;

    @JsonProperty("matching_service_adapters")
    protected List<RemoteMatchingServiceConfig> matchingServiceAdapters;

    @JsonProperty("service_providers")
    protected List<RemoteServiceProviderConfig> serviceProviders;

    public Date getPublishedAt() {
        return publishedAt;
    }

    public List<RemoteConnectedServiceConfig> getConnectedServices() {
        return connectedServices;
    }

    public List<RemoteMatchingServiceConfig> getMatchingServiceAdapters() {
        return matchingServiceAdapters;
    }

    public List<RemoteServiceProviderConfig> getServiceProviders() {
        return serviceProviders;
    }
}
