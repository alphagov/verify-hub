package uk.gov.ida.hub.config.data;

import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;

import java.io.IOException;

public class ConnectedServiceConfigRepository {
    private S3ConfigSource s3ConfigSource;

    public ConnectedServiceConfigRepository(S3ConfigSource s3ConfigSource) {
        this.s3ConfigSource = s3ConfigSource;
    }

    public RemoteConnectedServiceConfig get(String entityId) throws IOException {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        return remoteConfigCollection.getConnectedServices().get(entityId);
    }
}