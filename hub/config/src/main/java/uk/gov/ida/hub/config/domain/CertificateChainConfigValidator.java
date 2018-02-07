package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker;

public abstract class CertificateChainConfigValidator {
    private final CertificateValidityChecker certificateValidityChecker;
    private final EntityConfigDataToCertificateDtoTransformer certificateDtoTransformer;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateChainConfigValidator.class);

    @Inject
    public CertificateChainConfigValidator(final TrustStoreForCertificateProvider trustStoreForCertificateProvider, final CertificateChainValidator certificateChainValidator) {
        this.certificateValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
        this.certificateDtoTransformer = new EntityConfigDataToCertificateDtoTransformer();
    }

    public void validate(final Set<TransactionConfigEntityData> transactionConfigEntityData, final Set<MatchingServiceConfigEntityData> matchingServiceConfigEntityData) {
        Collection<CertificateDetails> certificateDetails = certificateDtoTransformer.transform(transactionConfigEntityData, matchingServiceConfigEntityData);
        ImmutableList<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(certificateDetails);

        handleInvalidCertificates(invalidCertificates);
    }

    abstract void handleInvalidCertificates(ImmutableList<InvalidCertificateDto> invalidCertificates);
}
