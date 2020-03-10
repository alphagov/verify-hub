package uk.gov.ida.hub.config.domain.remoteconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RemoteConfigCollection {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteConfigCollection.class);

    private Date lastModified;
    private Date publishedAt;
    private Map<String, RemoteConnectedServiceConfig> connectedServices;
    private Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters;
    private Map<String, RemoteServiceProviderConfig> serviceProviders;

    public static final RemoteConfigCollection EMPTY_REMOTE_CONFIG_COLLECTION = new RemoteConfigCollection(null);

    private RemoteConfigCollection(Date lastModified) {
        this(lastModified, null, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    private RemoteConfigCollection(Date lastModified, Date publishedAt, Map<String, RemoteConnectedServiceConfig> connectedServices,
                                   Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters,
                                   Map<String, RemoteServiceProviderConfig> serviceProviders) {
        this.lastModified = lastModified;
        this.publishedAt = publishedAt;
        this.connectedServices = connectedServices;
        this.matchingServiceAdapters = matchingServiceAdapters;
        this.serviceProviders = serviceProviders;
    }

    public RemoteConfigCollection(Date lastModified, SelfServiceMetadata metadata) {
        this.lastModified = lastModified;
        this.publishedAt = metadata.getPublishedAt();
        this.serviceProviders = metadata.serviceProviders
                .stream()
                .collect(Collectors.toUnmodifiableMap(RemoteServiceProviderConfig::getId, v -> v));
        this.matchingServiceAdapters = metadata.matchingServiceAdapters
                .stream()
                .collect(Collectors.toUnmodifiableMap(RemoteMatchingServiceConfig::getEntityId, v -> v));
        this.connectedServices = metadata.connectedServices
                .stream()
                .map(v -> v.withServiceProviderConfig(getServiceProvider(v.getServiceProviderConfigId())))
                .collect(Collectors.toUnmodifiableMap(RemoteConnectedServiceConfig::getEntityId, v -> v));
    }

    private RemoteServiceProviderConfig getServiceProvider(String id){
        RemoteServiceProviderConfig result = serviceProviders.get(id);
        return result;
    }

    public Date getLastModified() {
        return lastModified;
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

    public Map<String, RemoteServiceProviderConfig> getServiceProviders() {
        return serviceProviders;
    }

    public Optional<RemoteComponentConfig> getRemoteComponent(CertificateConfigurable entity) {
        switch (entity.getEntityType()) {
            case RP:
                return Optional.ofNullable(connectedServices.get(entity.getEntityId()))
                        .map(RemoteConnectedServiceConfig::getServiceProviderConfig);
            case MS:
                return Optional.ofNullable(matchingServiceAdapters.get(entity.getEntityId()));
            default:
                LOG.warn("Entity ({}) has a type of '{}' that cannot be served by the remote config",
                        entity.getEntityId(),
                        entity.getEntityType());
                return Optional.empty();
        }
    }
    
    
}
