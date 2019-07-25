package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import uk.gov.ida.hub.config.data.LocalConfigRepository;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

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

    public Set<CertificateDetails> getAllCertificateDetails() {
        return Stream.concat(connectedServiceConfigRepository.stream(), matchingServiceConfigRepository.stream())
                .flatMap(this::getAllCertificateDetails)
                .collect(Collectors.toSet());
    }

    public  CertificateDetails encryptionCertificateFor(String entityId) {
        T config = getConfig(entityId);
        CertificateDetails certDetails = createCertificateDetails(config, config.getEncryptionCertificate());
        if (!certificateValidityChecker.isValid(certDetails)){
            throw new NoCertificateFoundException();
        }
        if (certDetails.isNotEnabled()){
            throw new CertificateDisabledException();
        }
        return certDetails;
    }

    public List<CertificateDetails> signatureVerificationCertificatesFor(String entityId) {
        T config = getConfig(entityId);
        return config.getSignatureVerificationCertificates()
                .stream()
                .map(cert -> createCertificateDetails(config, cert))
                .filter(cd -> certificateValidityChecker.isValid(cd))
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::throwIfEmpty));
    }

    private List<CertificateDetails> throwIfEmpty(List<CertificateDetails> list){
        if (list.isEmpty()){
            throw new NoCertificateFoundException();
        }
        return list;
    }

    private Stream<CertificateDetails> getAllCertificateDetails(LocalConfigRepository<T> configRepository) {
        return configRepository.getAllData()
                .stream()
                .flatMap(this::getAllCertificateDetails);
    }

    private Stream<CertificateDetails> getAllCertificateDetails(T config){
        List<CertificateDetails> certDetails = config.getSignatureVerificationCertificates()
                .stream()
                .map(cert -> createCertificateDetails(config, cert))
                .collect(Collectors.toList());
        certDetails.add(createCertificateDetails(config, config.getEncryptionCertificate()));
        return certDetails.stream();
    }

    private CertificateDetails createCertificateDetails(T config, Certificate certificate){
        return new CertificateDetails(
                config.getEntityId(),
                certificate,
                config.getEntityType(),
                config.isEnabled());
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