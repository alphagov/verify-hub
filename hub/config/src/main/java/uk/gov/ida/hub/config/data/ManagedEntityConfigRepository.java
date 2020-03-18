package uk.gov.ida.hub.config.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateOrigin;
import uk.gov.ida.hub.config.domain.remoteconfig.RemoteComponentConfig;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;
import javax.inject.Inject;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagedEntityConfigRepository<T extends CertificateConfigurable<T>> implements ConfigRepository<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedEntityConfigRepository.class);
    private final LocalConfigRepository<T> localConfigRepository;
    private final S3ConfigSource s3ConfigSource;

    @Inject
    public ManagedEntityConfigRepository(LocalConfigRepository<T> localConfigRepository, S3ConfigSource s3ConfigSource) {
        this.localConfigRepository = localConfigRepository;
        this.s3ConfigSource = s3ConfigSource;
    }

    public Collection<T> getAll() {
        return localConfigRepository.stream()
                .map(this::overrideWithRemote)
                .collect(Collectors.toList());
    }

    public boolean has(String entityId){
        return localConfigRepository.containsKey(entityId);
    }

    public Optional<T> get(String entityId) {
        return localConfigRepository.getData(entityId)
                .map(this::overrideWithRemote);
    }

    public Stream<T> stream() {
        return localConfigRepository.stream();
    }
    

    private T overrideWithRemote(T local) {
        if (!local.isSelfService()) return local;
        
        return s3ConfigSource.getRemoteConfig()
                .getRemoteComponent(local)
                .map(remote -> getRemoteOverrides(local, remote))
                .orElseGet(() -> logWarningAndReturnUnOverriddenConfig(local));
    }

    private T logWarningAndReturnUnOverriddenConfig(T local) {
        LOG.warn("Local config for '{}' expects there to be remote config but it could not be found", local.getEntityId());
        return local;
    }

    private T getRemoteOverrides(T local, RemoteComponentConfig remote) {
        return local.override(
                getSignatureVerificationCertificates(local, remote),
                getEncryptionCertificate(local, remote),
                CertificateOrigin.SELFSERVICE);
    }

    private List<String> getSignatureVerificationCertificates(T local, RemoteComponentConfig remote) {
        try {
            return remote.getSignatureVerificationCertificates();
        }
        catch (NullPointerException e){
            throw new NoCertificateFoundException(MessageFormat.format("Remote config signing certificates missing for {} {}",
                    local.getEntityId(),
                    Optional.ofNullable(local.getSignatureVerificationCertificates()
                            .stream().map(Certificate::getSubject)
                            .reduce("", String::join))));
        }
    }

    private String getEncryptionCertificate(T local, RemoteComponentConfig remote) {
        try {
            return remote.getEncryptionCertificate();
        }
        catch (NullPointerException e){
            throw new NoCertificateFoundException(MessageFormat.format("Remote config encryption certificate missing for {} {}",
                    local.getEntityId(),
                    local.getEncryptionCertificate().getX509Certificate()
                            .map(X509Certificate::getSubjectDN)
                            .map(Principal::getName)
                            .orElseGet(String::new)));
        }
    }
}
