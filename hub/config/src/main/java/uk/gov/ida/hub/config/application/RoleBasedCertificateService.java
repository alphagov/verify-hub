package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.EncryptionCertificate;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class RoleBasedCertificateService<T extends CertificateConfigurable<T>> {

    private final ManagedEntityConfigRepository<T> managedEntityConfigRepository;
    private CertificateValidityChecker certificateValidityChecker;

    @Inject
    public RoleBasedCertificateService(
        ManagedEntityConfigRepository<T> managedEntityConfigRepository,
        CertificateValidityChecker certificateValidityChecker
    ) {
        this.managedEntityConfigRepository = managedEntityConfigRepository;
        this.certificateValidityChecker = certificateValidityChecker;
    }

    public Set<CertificateDetails> getAllCertificateDetails() {
        return managedEntityConfigRepository.getAll()
            .stream()
            .flatMap(this::getAllCertificateDetails)
            .collect(Collectors.toSet());
    }

    public Optional<CertificateDetails> encryptionCertificateFor(String entityId) {
        return getConfig(entityId).map(this::createEncryptionCertificateDetails);
    }

    public Optional<List<CertificateDetails>> signatureVerificationCertificatesFor(String entityId) {
        return getConfig(entityId)
            .map(this::createSigningCertificateDetails);
    }

    private CertificateDetails createEncryptionCertificateDetails(T config) {
        EncryptionCertificate encryptionCertificate = config.getEncryptionCertificate();
        CertificateDetails certificateDetails = createCertificateDetails(config, encryptionCertificate);
        if (!certificateValidityChecker.isValid(certificateDetails)){
            throw new NoCertificateFoundException();
        }
        if (certificateDetails.isNotEnabled()){
            throw new CertificateDisabledException();
        }
        return certificateDetails;
    }

    private List<CertificateDetails> createSigningCertificateDetails(T config) {
        return config.getSignatureVerificationCertificates()
            .stream()
            .map(cert -> createCertificateDetails(config, cert))
            .filter((details) -> certificateValidityChecker.isValid(details))
            .collect(Collectors.collectingAndThen(Collectors.toList(), this::throwIfEmpty));
    }

    private CertificateDetails createCertificateDetails(T config, Certificate certificate) {
        return new CertificateDetails(
            config.getEntityId(),
            certificate,
            config.getEntityType(),
            config.isEnabled());
    }


    private List<CertificateDetails> throwIfEmpty(List<CertificateDetails> list){
        if (list.isEmpty()){
            throw new NoCertificateFoundException();
        }
        return list;
    }

    private Stream<CertificateDetails> getAllCertificateDetails(T config){
        List<CertificateDetails> certDetails = config.getSignatureVerificationCertificates()
            .stream()
            .map(cert -> createCertificateDetails(config, cert))
            .collect(Collectors.toList());
        certDetails.add(createCertificateDetails(config, config.getEncryptionCertificate()));
        return certDetails.stream();
    }

    private Optional<T> getConfig(String entityId){
        return managedEntityConfigRepository.get(entityId);
    }

};
