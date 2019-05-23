package uk.gov.ida.hub.config.data;

import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;

public class MatchingServiceConfigRepository {

    private S3ConfigSource s3ConfigSource;

    public MatchingServiceConfigRepository(S3ConfigSource s3ConfigSource) {
        this.s3ConfigSource = s3ConfigSource;
    }

    public RemoteMatchingServiceConfig get(String entityId) {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        return remoteConfigCollection.getMatchingServiceAdapters().get(entityId);
    }
}

