package uk.gov.ida.hub.config.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteComponentConfig;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteConfigCollection;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManagedEntityConfigRepository<T extends CertificateConfigurable<T>> implements ConfigRepository<T>{

    private static final Logger LOG = LoggerFactory.getLogger(ManagedEntityConfigRepository.class);
    private LocalConfigRepository<T> localConfigRepository;
    private S3ConfigSource s3ConfigSource;

    @Inject
    public ManagedEntityConfigRepository(LocalConfigRepository<T> localConfigRepository, S3ConfigSource s3ConfigSource) {
        this.localConfigRepository = localConfigRepository;
        this.s3ConfigSource = s3ConfigSource;
    }

    public List<T> getAll() {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        return localConfigRepository.stream()
            .map((local) -> overrideWithRemote(local, remoteConfigCollection))
            .collect(Collectors.toList());
    }

    public boolean has(String entityId){
        return localConfigRepository.containsKey(entityId);
    }

    public Optional<T> get(String entityId) {
        RemoteConfigCollection remoteConfigCollection = s3ConfigSource.getRemoteConfig();
        Optional<T> localConfig = localConfigRepository.getData(entityId);
        return localConfig.map(local -> overrideWithRemote(local, remoteConfigCollection));
    }

    private T overrideWithRemote(T local, RemoteConfigCollection remoteConfigCollection){
        if (local.isSelfService()){
            Optional<RemoteComponentConfig> remoteComponentConfigOptional = remoteConfigCollection.getRemoteComponent(local);
            if (remoteComponentConfigOptional.isPresent()) {
                RemoteComponentConfig remoteComponentConfig = remoteComponentConfigOptional.get();
                return local.override(
                    remoteComponentConfig.getSignatureVerificationCertificates(),
                    remoteComponentConfig.getEncryptionCertificate()
                );
            } else {
                LOG.error("Local config for '{}' expects there to be remote config but it could not be found", local.getEntityId());
            }
        }
        return local;
    }
}