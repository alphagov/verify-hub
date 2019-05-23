package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConfigCollection {

    @JsonProperty("published_at")
    protected Date publishedAt;

    @JsonProperty("connected_services")
    protected Map<String, RemoteConnectedServiceConfig> connectedServices;

    @JsonProperty("matching_service_adapters")
    protected Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters;

    @JsonProperty("service_providers")
    protected List<RemoteServiceProviderConfig> serviceProviders;

    public RemoteConfigCollection() {

    }

    public RemoteConfigCollection(Date publishedAt, Map<String, RemoteConnectedServiceConfig> connectedServices,
                                  Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters,
                                  List<RemoteServiceProviderConfig> serviceProviders) {
        this.publishedAt = publishedAt;
        this.connectedServices = connectedServices;
        this.matchingServiceAdapters = matchingServiceAdapters;
        this.serviceProviders = serviceProviders;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public Map<String, RemoteConnectedServiceConfig> getConnectedServices() {
        return connectedServices;
    }

    public Map<String, RemoteMatchingServiceConfig> getMatchingServiceAdapters() {
        return matchingServiceAdapters;
    }

    public List<RemoteServiceProviderConfig> getServiceProviders() {
        return serviceProviders;
    }
}
