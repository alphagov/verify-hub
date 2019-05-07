package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.EntityIdentifiable;
import uk.gov.ida.hub.config.data.ConfigRepository;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class CertificateService {

    private static final List<CertificateDetails> EMPTY = null;
    private final ConfigRepository<TransactionConfig> transactionDataSource;
    private final ConfigRepository<MatchingServiceConfig> matchingServiceDataSource;
    private CertificateValidityChecker certificateValidityChecker = null;

    private final Function<String, Optional<CertificateDetails>> getTransactionEncryptionCert;
    private final Function<String, Optional<CertificateDetails>> getMatchingServiceEncryptionCert;
    private final Function<String, Optional<List<CertificateDetails>>> getTransactionSignatureCert;
    private final Function<String, Optional<List<CertificateDetails>>> getMatchingServiceSignatureCert;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateService.class);

    @Inject
    public CertificateService(
            ConfigRepository<TransactionConfig> transactionDataSource,
            ConfigRepository<MatchingServiceConfig> matchingServiceDataSource,
            CertificateValidityChecker certificateValidityChecker) {
        this.transactionDataSource = transactionDataSource;
        this.matchingServiceDataSource = matchingServiceDataSource;
        this.certificateValidityChecker = certificateValidityChecker;

        getTransactionEncryptionCert = encryptiondataSource.apply(transactionDataSource, FederationEntityType.RP);
        getMatchingServiceEncryptionCert = encryptiondataSource.apply(matchingServiceDataSource, FederationEntityType.MS);
        getTransactionSignatureCert = signatureDataSource.apply(transactionDataSource, FederationEntityType.RP);
        getMatchingServiceSignatureCert = signatureDataSource.apply(matchingServiceDataSource, FederationEntityType.MS);
    }

    public CertificateDetails encryptionCertificateFor(String entityId) {
        CertificateDetails certificateDetails = getTransactionEncryptionCert.apply(entityId)
                .orElseGet(() -> getMatchingServiceEncryptionCert.apply(entityId)
                .orElseThrow(() -> new NoCertificateFoundException()));

        if (certificateDetails.isNotEnabled()) {
            throw new CertificateDisabledException();
        }

        return certificateDetails;
    }

    public Set<CertificateDetails> getAllCertificatesDetails() {
        Set<CertificateDetails> certificateDetailsSet = new HashSet<>();
        certificateDetailsSet.addAll(getCertificatesDetailsSet(transactionDataSource, FederationEntityType.RP));
        certificateDetailsSet.addAll(getCertificatesDetailsSet(matchingServiceDataSource, FederationEntityType.MS));
        return certificateDetailsSet;
    }

    private <T extends EntityIdentifiable & CertificateConfigurable> Set<CertificateDetails> getCertificatesDetailsSet(final ConfigRepository<T> configRepository,
                                                                                                                       final FederationEntityType federationEntityType) {
        Set<CertificateDetails> certificateDetailsSet = new HashSet<>();
        final Set<T> configs = configRepository.getAllData();
        configs.forEach(
            config -> {
                config.getSignatureVerificationCertificates()
                      .forEach(certificate -> certificateDetailsSet.add(new CertificateDetails(
                          config.getEntityId(),
                          certificate,
                          federationEntityType,
                          config.isEnabled())));
                certificateDetailsSet.add(new CertificateDetails(
                    config.getEntityId(),
                    config.getEncryptionCertificate(),
                    federationEntityType,
                    config.isEnabled()));
            });
        return certificateDetailsSet;
    }

    public List<CertificateDetails> signatureVerificatonCertificatesFor(String entityId) {
        return getTransactionSignatureCert.apply(entityId)
                .orElseGet(() -> getMatchingServiceSignatureCert.apply(entityId)
                .orElseThrow(() -> new NoCertificateFoundException()));
    }

    private BiFunction<ConfigRepository<? extends CertificateConfigurable>, FederationEntityType,
            Function<String, Optional<CertificateDetails>>> encryptiondataSource =
            (certEntityRepo, fedType) -> entityId ->
                    certEntityRepo.getData(entityId)
                    .map(cert -> new CertificateDetails(entityId, cert.getEncryptionCertificate(), fedType, cert.isEnabled()))
                    .filter(cert -> certificateValidityChecker.isValid(cert));

    private BiFunction<ConfigRepository<? extends CertificateConfigurable>, FederationEntityType,
            Function<String, Optional<List<CertificateDetails>>>> signatureDataSource =
            (certEntityRepo, fedType) -> entityId ->
                certEntityRepo.getData(entityId)
                .map(cert -> cert.getSignatureVerificationCertificates())
                .map(sigCerts -> sigCerts.stream()
                        .map(cert -> new CertificateDetails(entityId, cert, fedType))
                        .filter(certDetail -> certificateValidityChecker.isValid(certDetail))
                        .collect(collectingAndThen(toList(), certDetailsList -> certDetailsList.isEmpty() ? EMPTY : certDetailsList)));
}

