package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import uk.gov.ida.hub.config.data.ConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.EntityIdentifiable;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertificateService <T extends EntityIdentifiable & CertificateConfigurable> {

    private final List<ConfigRepository<T>> configRepositories =new ArrayList<>();
    private CertificateValidityChecker certificateValidityChecker;
//    private static final Logger LOG = LoggerFactory.getLogger(CertificateService.class);

    @Inject
    public CertificateService(
            ConfigRepository<TransactionConfig> transactionConfigRepository,
            ConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository,
            CertificateValidityChecker certificateValidityChecker) {
        this.configRepositories.add((ConfigRepository<T>)transactionConfigRepository);
        this.configRepositories.add((ConfigRepository<T>)matchingServiceConfigRepository);
        this.certificateValidityChecker = certificateValidityChecker;
    }

    public Set<CertificateDetails> getAllCertificateDetails() {
        return configRepositories.stream()
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

    private Stream<CertificateDetails> getAllCertificateDetails(ConfigRepository<T> configRepository) {
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
                getType(config),
                config.isEnabled());
    }

    private T getConfig(String entityId){
        return configRepositories.stream()
                .filter(repo -> repo.containsKey(entityId))
                .findFirst()
                .orElseThrow(NoCertificateFoundException::new)
                .getData(entityId)
                .get();
    }

    private FederationEntityType getType(T config) {
        return config instanceof TransactionConfig ? FederationEntityType.RP : FederationEntityType.MS;
    }
}