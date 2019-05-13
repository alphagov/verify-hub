package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteConfigCollection {

    @Valid
    @NotNull
    @JsonProperty
    protected Instant publishedAt;

    @Valid
    @NotNull
    @JsonProperty
    protected List<RemoteConnectedServiceConfig> connectedServices;

    @Valid
    @NotNull
    @JsonProperty
    protected List<RemoteMatchingServiceConfig> matchingServiceAdapters;

    @Valid
    @NotNull
    @JsonProperty
    protected List<RemoteServiceProviderConfig> serviceProviders;


    @SuppressWarnings("unused")
    protected RemoteConfigCollection() {
    }

    public Instant getPublishedAt() {
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
