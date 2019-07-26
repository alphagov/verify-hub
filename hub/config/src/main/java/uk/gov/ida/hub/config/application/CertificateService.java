package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertificateService <T extends CertificateConfigurable<T>> {

    private final ManagedEntityConfigRepository<T> connectedServiceConfigRepository;
    private final ManagedEntityConfigRepository<T> matchingServiceConfigRepository;
    private CertificateValidityChecker certificateValidityChecker;

    @Inject
    public CertificateService(
            ManagedEntityConfigRepository<T> connectedServiceConfigRepository,
            ManagedEntityConfigRepository<T> matchingServiceConfigRepository,
            CertificateValidityChecker certificateValidityChecker) {
        this.connectedServiceConfigRepository = connectedServiceConfigRepository;
        this.matchingServiceConfigRepository = matchingServiceConfigRepository;
        this.certificateValidityChecker = certificateValidityChecker;
    }

    public Set<Certificate> getAllCertificates() {
        return Stream.concat(connectedServiceConfigRepository.stream(), matchingServiceConfigRepository.stream())
                .flatMap(this::getAllCertificates)
                .collect(Collectors.toSet());
    }

    public  Certificate encryptionCertificateFor(String entityId) {
        T config = getConfig(entityId);
        Certificate cert = config.getEncryptionCertificate();
        if (!certificateValidityChecker.isValid(cert)){
            throw new NoCertificateFoundException();
        }
        if (!cert.isEnabled()){
            throw new CertificateDisabledException();
        }
        return cert;
    }

    public List<Certificate> signatureVerificationCertificatesFor(String entityId) {
        T config = getConfig(entityId);
        return config.getSignatureVerificationCertificates()
                .stream()
                .filter(cd -> certificateValidityChecker.isValid(cd))
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::throwIfEmpty));
    }

    private List<Certificate> throwIfEmpty(List<Certificate> list){
        if (list.isEmpty()){
            throw new NoCertificateFoundException();
        }
        return list;
    }

    private Stream<Certificate> getAllCertificates(LocalConfigRepository<T> configRepository) {
        return configRepository.getAllData()
                .stream()
                .flatMap(this::getAllCertificates);
    }

    private Stream<Certificate> getAllCertificates(T config){
        List<Certificate> certs = new ArrayList();
        certs.addAll(config.getSignatureVerificationCertificates());
        certs.add(config.getEncryptionCertificate());
        return certs.stream();
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