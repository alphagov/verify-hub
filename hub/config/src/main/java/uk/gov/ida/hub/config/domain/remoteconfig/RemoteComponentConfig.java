package uk.gov.ida.hub.config.domain.remoteconfig;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface RemoteComponentConfig {

    RemoteCertificateConfig getEncryptionCertificateConfig();
    List<RemoteCertificateConfig> getSigningCertificatesConfig();

    default List<String> getSignatureVerificationCertificates() {
        return getSigningCertificatesConfig().stream()
                .map(RemoteCertificateConfig::getValue)
                .collect(Collectors.toList());
    }

    default String getEncryptionCertificate() {
        return Optional.of(getEncryptionCertificateConfig())
                .map(RemoteCertificateConfig::getValue)
                .get();
    }
}