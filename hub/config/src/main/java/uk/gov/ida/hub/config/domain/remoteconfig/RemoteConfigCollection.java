package uk.gov.ida.hub.config.domain.remoteconfig;

import uk.gov.ida.hub.config.domain.CertificateConfigurable;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class RemoteConfigCollection {

    private Date publishedAt;

    private Map<String, ConnectedService> connectedServices;

    private Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters;

    public RemoteConfigCollection(Date publishedAt,
                                  Map<String, ConnectedService> connectedServices,
                                  Map<String, RemoteMatchingServiceConfig> matchingServiceAdapters
                                  ) {
        this.publishedAt = publishedAt;
        this.connectedServices = connectedServices;
        this.matchingServiceAdapters = matchingServiceAdapters;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public Map<String, RemoteMatchingServiceConfig> getMatchingServiceAdapters() {
        return matchingServiceAdapters;
    }

    public Map<String, ConnectedService> getConnectedServices() {
        return connectedServices;
    }

    public Optional<RemoteComponentConfig> getRemoteComponent(CertificateConfigurable entity) {
        switch (entity.getEntityType()) {
            case RP:
                return Optional.ofNullable(connectedServices.get(entity.getEntityId()))
                    .map(ConnectedService::getServiceProviderConfig);
            case MS:
                return Optional.ofNullable(matchingServiceAdapters.get(entity.getEntityId()));
            default:
                throw new RuntimeException(
                    String.format(
                        "Entity (%s) has a type of '%s' that cannot be served by the remote config",
                        entity.getEntityId(),
                        entity.getEntityType()
                    )
                );
        }
    }

}
