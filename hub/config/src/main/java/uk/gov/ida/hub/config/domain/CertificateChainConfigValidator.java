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
import java.util.stream.Collectors;

import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker;

public abstract class CertificateChainConfigValidator {
    private final CertificateValidityChecker certificateValidityChecker;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateChainConfigValidator.class);

    @Inject
    public CertificateChainConfigValidator(final TrustStoreForCertificateProvider trustStoreForCertificateProvider, final CertificateChainValidator certificateChainValidator) {
        this.certificateValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
    }

    public void validate(Set<CertificateConfigurable<? extends CertificateConfigurable<?>>> configs) {
        Collection<Certificate> certificates = configs
                .stream()
                .flatMap(config -> config.getAllCertificates().stream())
                .collect(Collectors.toList());

        ImmutableList<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(certificates);
        handleInvalidCertificates(invalidCertificates);
    }

    abstract void handleInvalidCertificates(ImmutableList<InvalidCertificateDto> invalidCertificates);

}
