package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.stream.Collectors;

public class LoggingCertificateChainConfigValidator extends CertificateChainConfigValidator {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingCertificateChainConfigValidator.class);

    @Inject
    public LoggingCertificateChainConfigValidator(final TrustStoreForCertificateProvider trustStoreForCertificateProvider, final CertificateChainValidator certificateChainValidator) {
        super(trustStoreForCertificateProvider, certificateChainValidator);
    }

    @Override
    void handleInvalidCertificates(ImmutableList<InvalidCertificateDto> invalidCertificates) {
        if (!invalidCertificates.isEmpty()) {
            LOG.info(invalidCertificates.stream().map(certificate -> MessageFormat.format(
                    "Invalid certificate found.\nEntity Id: {0}\nCertificate Type: {1}\nFederation Type: {2}\nReason: {3}\nDescription: {4}",
                    certificate.getEntityId(),
                    certificate.getCertificateType(),
                    certificate.getFederationType(),
                    certificate.getReason(),
                    certificate.getDescription())).collect(Collectors.joining("\n")));
        }

    }
}
