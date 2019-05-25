package uk.gov.ida.hub.config.data;

import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteMatchingServiceConfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MatchingServiceConfigRepository implements ConfigRepository<MatchingServiceConfig>{

    private LocalConfigRepository<MatchingServiceConfig> localConfigRepository;
    private S3ConfigSource s3ConfigSource;

    public MatchingServiceConfigRepository(LocalConfigRepository<MatchingServiceConfig> localConfigRepository, S3ConfigSource s3ConfigSource) {
        this.localConfigRepository = localConfigRepository;
        this.s3ConfigSource = s3ConfigSource;
    }

    public List<MatchingServiceConfig> getAll() {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        return localConfigRepository.getAllData().stream()
                .map(local -> overrideWithRemote(local, remoteConfigCollection))
                .collect(Collectors.toList());
    }

    public Optional<MatchingServiceConfig> get(String entityId) {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        Optional<MatchingServiceConfig> localConfig = localConfigRepository.getData(entityId);
        return localConfig.map(local -> overrideWithRemote(local, remoteConfigCollection));
    }

    public boolean has(String entityId){
        return localConfigRepository.containsKey(entityId);
    }

    private MatchingServiceConfig overrideWithRemote(MatchingServiceConfig local, RemoteConfigCollection remoteConfigCollection){
        if (local.isSelfService()){
            RemoteMatchingServiceConfig remote = remoteConfigCollection.getMatchingServiceAdapters().get(local.getEntityId());
            if (remote != null) {
                return local.override(remote);
            }
        }
        return local;
    }

}

