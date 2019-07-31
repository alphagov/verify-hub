package uk.gov.ida.hub.config.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker;

public abstract class CertificateChainConfigValidator {
    private final CertificateValidityChecker certificateValidityChecker;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateChainConfigValidator.class);

    @Inject
    public CertificateChainConfigValidator(final TrustStoreForCertificateProvider trustStoreForCertificateProvider, final CertificateChainValidator certificateChainValidator) {
        this.certificateValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
    }

    public void validate(final Collection<TransactionConfig> transactionConfigs, final Collection<MatchingServiceConfig> matchingServiceConfigs) {
        Collection<Certificate> certificates = getCertificates(transactionConfigs, matchingServiceConfigs);
        Set<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(certificates);
        handleInvalidCertificates(invalidCertificates);
    }

    abstract void handleInvalidCertificates(Collection<InvalidCertificateDto> invalidCertificates);

    private Collection<Certificate> getCertificates(Collection<TransactionConfig> transactionConfigs, Collection<MatchingServiceConfig> matchingServiceConfigs) {
        return Stream.concat(transactionConfigs.stream(), matchingServiceConfigs.stream())
                .flatMap(config -> config.getAllCertificates().stream())
                .collect(Collectors.toSet());
    }
}
