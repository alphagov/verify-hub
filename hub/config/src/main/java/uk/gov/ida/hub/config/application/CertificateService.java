package uk.gov.ida.hub.config.application;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.CertificateEntity;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public class CertificateService {

    private static final List<CertificateDetails> EMPTY = null;
    private final ConfigEntityDataRepository<TransactionConfigEntityData> transactionDataSource;
    private final ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceDataSource;
    private CertificateValidityChecker certificateValidityChecker = null;

    private final Function<String, Optional<CertificateDetails>> getTransactionEncryptionCert;
    private final Function<String, Optional<CertificateDetails>> getMatchingServiceEncryptionCert;
    private final Function<String, Optional<List<CertificateDetails>>> getTransactionSignatureCert;
    private final Function<String, Optional<List<CertificateDetails>>> getMatchingServiceSignatureCert;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateService.class);

    @Inject
    public CertificateService(
            ConfigEntityDataRepository<TransactionConfigEntityData> transactionDataSource,
            ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceDataSource,
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

    public List<CertificateDetails> signatureVerificatonCertificatesFor(String entityId) {
        return getTransactionSignatureCert.apply(entityId)
                .orElseGet(() -> getMatchingServiceSignatureCert.apply(entityId)
                .orElseThrow(() -> new NoCertificateFoundException()));
    }

    private BiFunction<ConfigEntityDataRepository<? extends CertificateEntity>, FederationEntityType,
            Function<String, Optional<CertificateDetails>>> encryptiondataSource =
            (certEntityRepo, fedType) -> entityId ->
                    certEntityRepo.getData(entityId)
                    .map(cert -> new CertificateDetails(entityId, cert.getEncryptionCertificate(), fedType, cert.isEnabled()))
                    .filter(cert -> certificateValidityChecker.isValid(cert));

    private BiFunction<ConfigEntityDataRepository<? extends CertificateEntity>, FederationEntityType,
            Function<String, Optional<List<CertificateDetails>>>> signatureDataSource =
            (certEntityRepo, fedType) -> entityId ->
                certEntityRepo.getData(entityId)
                .map(CertificateEntity::getSignatureVerificationCertificates)
                .map(sigCerts -> sigCerts.stream()
                        .map(cert -> new CertificateDetails(entityId, cert, fedType))
                        .filter(cert -> certificateValidityChecker.isValid(cert))
                        .collect(collectingAndThen(toList(), certDetailsList -> certDetailsList.isEmpty() ? EMPTY : certDetailsList)));
}

