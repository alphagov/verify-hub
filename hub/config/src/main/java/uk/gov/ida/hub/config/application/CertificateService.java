package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertificateService <T extends CertificateConfigurable<T>> {

    private final ManagedEntityConfigRepository<T> connectedServiceConfigRepository;
    private final ManagedEntityConfigRepository<T> matchingServiceConfigRepository;

    @Inject
    public CertificateService(
            ManagedEntityConfigRepository<T> connectedServiceConfigRepository,
            ManagedEntityConfigRepository<T> matchingServiceConfigRepository) {
        this.connectedServiceConfigRepository = connectedServiceConfigRepository;
        this.matchingServiceConfigRepository = matchingServiceConfigRepository;
    }

    public Set<Certificate> getAllCertificates() {
        return Stream.concat(connectedServiceConfigRepository.stream(), matchingServiceConfigRepository.stream())
                .flatMap(config -> config.getAllCertificates().stream())
                .collect(Collectors.toSet());
    }

    public  Certificate encryptionCertificateFor(String entityId) {
        T config = getConfig(entityId);
        Certificate cert = config.getEncryptionCertificate();
        if (!cert.isEnabled()){
            throw new CertificateDisabledException();
        }
        return cert;
    }

    public List<Certificate> signatureVerificationCertificatesFor(String entityId) {
        T config = getConfig(entityId);
        return config.getSignatureVerificationCertificates()
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::throwIfEmpty));
    }

    private List<Certificate> throwIfEmpty(List<Certificate> list){
        if (list.isEmpty()){
            throw new NoCertificateFoundException();
        }
        return list;
    }

    private T getConfig(String entityId){
        return Stream.of(connectedServiceConfigRepository, matchingServiceConfigRepository)
                .filter(repo -> repo.has(entityId))
                .findFirst()
                .orElseThrow(NoCertificateFoundException::new)
                .get(entityId)
                .get();
    }
}