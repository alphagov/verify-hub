package uk.gov.ida.hub.config.domain;

import uk.gov.ida.common.shared.security.verification.OCSPCertificateChainValidator;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createOCSPCheckingCertificateValidityChecker;

public class OCSPCertificateChainValidityChecker {
    private final CertificateValidityChecker certificateValidityChecker;

    @Inject
    public OCSPCertificateChainValidityChecker(OCSPCertificateChainValidator ocspCertificateChainValidator, TrustStoreForCertificateProvider trustStoreForCertificateProvider) {
        this.certificateValidityChecker = createOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, ocspCertificateChainValidator);
    }

    public Set<InvalidCertificateDto> check(Collection<Certificate> certificate) {
        return certificateValidityChecker.getInvalidCertificates(certificate);
    }

    public boolean isValid(final Certificate certificate) {
        return certificateValidityChecker.isValid(certificate);
    }
}
