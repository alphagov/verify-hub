package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConfigCollection {

    @JsonProperty("published_at")
    protected String publishedAt;

    @JsonProperty("event_id")
    protected int eventId;

    @JsonProperty("connected_services")
    protected List<RemoteConnectedServiceConfig> connectedServices;

    @JsonProperty("matching_service_adapters")
    protected List<RemoteMatchingServiceConfig> matchingServiceAdapters;

    @JsonProperty("service_providers")
    protected List<RemoteServiceProviderConfig> serviceProviders;


    @SuppressWarnings("unused")
    protected RemoteConfigCollection() {
    }

    public String getPublishedAt() {
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

    public int getEventId() {
        return eventId;
    }
}
