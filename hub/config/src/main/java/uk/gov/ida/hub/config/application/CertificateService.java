package uk.gov.ida.hub.config.application;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.List;
import java.util.Set;

public class CertificateService {
    private final RoleBasedCertificateService<TransactionConfig> transactionCertificateService;
    private final RoleBasedCertificateService<MatchingServiceConfig> matchingServiceConfigRepository;

    @Inject
    public CertificateService(
        RoleBasedCertificateService<MatchingServiceConfig> matchingServiceConfigRepository,
        RoleBasedCertificateService<TransactionConfig> transactionCertificateService
    ) {
        this.transactionCertificateService = transactionCertificateService;
        this.matchingServiceConfigRepository = matchingServiceConfigRepository;
    }

    public Set<CertificateDetails> getAllCertificateDetails() {
        return ImmutableSet.<CertificateDetails>builder()
            .addAll(transactionCertificateService.getAllCertificateDetails())
            .addAll(matchingServiceConfigRepository.getAllCertificateDetails())
            .build();
    }

    public CertificateDetails encryptionCertificateFor(String entityId) {
        return transactionCertificateService.encryptionCertificateFor(entityId)
            .or(() -> matchingServiceConfigRepository.encryptionCertificateFor(entityId))
            .orElseThrow(NoCertificateFoundException::new);
    }

    public List<CertificateDetails> signatureVerificationCertificatesFor(String entityId) {
        return transactionCertificateService.signatureVerificationCertificatesFor(entityId)
            .or(() -> matchingServiceConfigRepository.signatureVerificationCertificatesFor(entityId))
            .orElseThrow(NoCertificateFoundException::new);
    }
}