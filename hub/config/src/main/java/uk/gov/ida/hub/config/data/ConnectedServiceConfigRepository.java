package uk.gov.ida.hub.config.data;

import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConnectedServiceConfig;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ConnectedServiceConfigRepository implements ConfigRepository<TransactionConfig>{

    private LocalConfigRepository<TransactionConfig> localConfigRepository;
    private S3ConfigSource s3ConfigSource;


    public ConnectedServiceConfigRepository(LocalConfigRepository<TransactionConfig> localConfigRepository, S3ConfigSource s3ConfigSource) {
        this.localConfigRepository = localConfigRepository;
        this.s3ConfigSource = s3ConfigSource;
    }

    public List<TransactionConfig> getAll() {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        return localConfigRepository.getAllData().stream()
                .map(local -> overrideWithRemote(local, remoteConfigCollection))
                .collect(toList());
    }

    public boolean has(String entityId){
        return localConfigRepository.containsKey(entityId);
    }

    public Optional<TransactionConfig> get(String entityId) {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        Optional<TransactionConfig> localConfig = localConfigRepository.getData(entityId);
        return localConfig.map(local -> overrideWithRemote(local, remoteConfigCollection));
    }

    private TransactionConfig overrideWithRemote(TransactionConfig local, RemoteConfigCollection remoteConfigCollection){
        if (local.isSelfService()){
            RemoteConnectedServiceConfig remote = remoteConfigCollection.getConnectedServices().get(local.getEntityId());
            if (remote != null) {
                return local.override(remote);
            }
        }
        return local;
    }

}