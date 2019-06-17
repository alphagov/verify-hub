package uk.gov.ida.hub.config.domain.remoteconfig;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoteConfigCollection {

    private Date lastModified;
    private Date publishedAt;
    private Map<String, RemoteConnectedServiceConfig> connectedServices;
    private Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters;
    private List<RemoteServiceProviderConfig> serviceProviders;

    public RemoteConfigCollection(Date lastModified, Date publishedAt, Map<String, RemoteConnectedServiceConfig> connectedServices,
                                  Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters,
                                  List<RemoteServiceProviderConfig> serviceProviders) {
        this.lastModified = lastModified;
        this.publishedAt = publishedAt;
        this.connectedServices = connectedServices;
        this.matchingServiceAdapters = matchingServiceAdapters;
        this.serviceProviders = serviceProviders;
    }

    public RemoteConfigCollection(Date lastModified, SelfServiceMetadata metadata) {
        matchingServiceAdapters = metadata.matchingServiceAdapters.stream().collect(Collectors.toUnmodifiableMap(RemoteMatchingServiceConfig::getEntityId, v->v));
        connectedServices = metadata.connectedServices.stream().collect(Collectors.toUnmodifiableMap(RemoteConnectedServiceConfig::getEntityId, v->v));
        serviceProviders = metadata.serviceProviders.stream().collect(Collectors.toUnmodifiableList());
        this.lastModified = lastModified;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
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
