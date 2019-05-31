package uk.gov.ida.hub.config.domain.remoteconfig;

import uk.gov.ida.hub.config.domain.X509CertificateConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface RemoteComponentConfig {

    RemoteCertificateConfig getEncryptionCertificateConfig();
    List<RemoteCertificateConfig> getSigningCertificatesConfig();

    default List<X509CertificateConfiguration> getSignatureVerificationCertificates() {
        return getSigningCertificatesConfig().stream()
            .map(RemoteCertificateConfig::getValue)
            .map(X509CertificateConfiguration::new)
            .collect(Collectors.toList());
}

    default X509CertificateConfiguration getEncryptionCertificate() {
        return Optional.of(getEncryptionCertificateConfig())
            .map(RemoteCertificateConfig::getValue)
            .map(X509CertificateConfiguration::new)
            .get();
    }
}
