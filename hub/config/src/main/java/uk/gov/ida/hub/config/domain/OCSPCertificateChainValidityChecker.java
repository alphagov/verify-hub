package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import uk.gov.ida.common.shared.security.verification.OCSPCertificateChainValidator;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import javax.inject.Inject;
import java.util.Collection;

import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createOCSPCheckingCertificateValidityChecker;

public class OCSPCertificateChainValidityChecker {
    private final CertificateValidityChecker certificateValidityChecker;

    @Inject
    public OCSPCertificateChainValidityChecker(OCSPCertificateChainValidator ocspCertificateChainValidator, TrustStoreForCertificateProvider trustStoreForCertificateProvider) {
        this.certificateValidityChecker = createOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, ocspCertificateChainValidator);
    }

    public ImmutableList<InvalidCertificateDto> check(Collection<CertificateDetails> certificateDetails) {
        return certificateValidityChecker.getInvalidCertificates(certificateDetails);
    }

    public boolean isValid(final Certificate certificate,
                           final FederationEntityType federationEntityType) {
        return certificateValidityChecker.isValid(certificate, federationEntityType);
    }
}
