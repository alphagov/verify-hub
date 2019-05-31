package uk.gov.ida.hub.config.domain.remoteconfig;

public class ConnectedService {

    private final RemoteConnectedServiceConfig connectedServiceConfig;
    private final RemoteServiceProviderConfig serviceProviderConfig;

    public ConnectedService(RemoteConnectedServiceConfig connectedServiceConfig, RemoteServiceProviderConfig serviceProviderConfig) {
        this.connectedServiceConfig = connectedServiceConfig;
        this.serviceProviderConfig = serviceProviderConfig;
    }

    public String getEntityId() {
        return connectedServiceConfig.getEntityId();
    }

    public RemoteServiceProviderConfig getServiceProviderConfig() {
        return serviceProviderConfig;
    }
}
