package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.partitioningBy;

public class CertificateService{

    private static final Logger LOG = LoggerFactory.getLogger(CertificateService.class);

    private final ManagedEntityConfigRepository<TransactionConfig> connectedServiceConfigRepository;
    private final ManagedEntityConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;
    private CertificateValidityChecker certificateValidityChecker;

    @Inject
    public CertificateService(
            ManagedEntityConfigRepository<TransactionConfig> connectedServiceConfigRepository,
            ManagedEntityConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository,
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
        CertificateConfigurable<?> config = getConfig(entityId);
        Certificate cert = config.getEncryptionCertificate();
        if (!certificateValidityChecker.isValid(cert)){
            LOG.warn("Encryption certificate for entityId '{}' was requested but is invalid", entityId);
            throw new NoCertificateFoundException();
        }
        if (!cert.isEnabled()){
            throw new CertificateDisabledException();
        }
        return cert;
    }

    public List<Certificate> signatureVerificationCertificatesFor(String entityId) {
        CertificateConfigurable<?> config = getConfig(entityId);

        Map<Boolean, List<Certificate>> certsByValidity = config.getSignatureVerificationCertificates()
                .stream()
                .collect(partitioningBy(cd -> certificateValidityChecker.isValid(cd)));

        int numberOfBadCerts = certsByValidity.get(false).size();
        String isOrAre = numberOfBadCerts == 1 ? "is" : "are";
        if (numberOfBadCerts > 0) {
            LOG.warn("Signature verification certificates were requested for entityId '{}'; {} of them {} invalid", entityId, numberOfBadCerts, isOrAre);
        }

        return certsByValidity.get(true)
                .stream()
                .collect(Collectors.collectingAndThen(Collectors.toList(), this::throwIfEmpty));
    }

    private List<Certificate> throwIfEmpty(List<Certificate> list){
        if (list.isEmpty()){
            throw new NoCertificateFoundException();
        }
        return list;
    }

    private Stream<Certificate> getAllCertificates(CertificateConfigurable<?> config){
        ArrayList<Certificate> certs = new ArrayList<>(config.getSignatureVerificationCertificates());
        certs.add(config.getEncryptionCertificate());
        return certs.stream();
    }

    private CertificateConfigurable<?> getConfig(String entityId){
        return Stream.of(connectedServiceConfigRepository, matchingServiceConfigRepository)
                .filter(repo -> repo.has(entityId))
                .findFirst()
                .orElseThrow(NoCertificateFoundException::new)
                .get(entityId)
                .orElseThrow();
    }
}