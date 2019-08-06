package uk.gov.ida.hub.config.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker;

public class CertificateChainConfigValidator {
    private final CertificateValidityChecker chainValidityChecker;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateChainConfigValidator.class);

    @Inject
    public CertificateChainConfigValidator(final TrustStoreForCertificateProvider trustStoreForCertificateProvider, final CertificateChainValidator certificateChainValidator) {
        this.chainValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
    }

    public void validate(Set<CertificateConfigurable<? extends CertificateConfigurable<?>>> configs) {
        configs.stream().forEach(this::validate);
    }

    private void validate(CertificateConfigurable<? extends CertificateConfigurable<?>> config){
        Map<Boolean, List<Certificate>> partition = config.getAllCertificates().stream()
                .collect(Collectors.partitioningBy(Certificate::isValid));

        partition.get(false)
                .stream()
                .forEach(cert -> validateMissing(config, cert));

        partition.get(true)
                .stream()
                .forEach(this::validateChain);

    }

    private void validateChain(Certificate certificate) {
        Optional<CertificateValidity> validity = chainValidityChecker.validate(certificate);
        validity.filter(Predicate.not(CertificateValidity::isValid))
                .ifPresent(cv -> logBadChainCertificate(certificate, cv));
    }

    private void validateMissing(CertificateConfigurable<? extends CertificateConfigurable<?>> config, Certificate certificate) {
        if(config.isSelfService() && !certificate.isValid()){
            logBadMissingCertificate(certificate);
        }
    }

    private void logBadMissingCertificate(Certificate certificate) {
        String message = MessageFormat.format(
                "Missing certificate in local config with Self Service not enabled.\nEntity Id: {0}\nCertificate Type: {1}\nFederation Type: {2}",
                certificate.getIssuerEntityId(),
                certificate.getCertificateUse(),
                certificate.getFederationEntityType());

        LOG.error(message);
    }

    private void logBadChainCertificate(Certificate certificate, CertificateValidity certificateValidity){
        String message = MessageFormat.format(
                "Invalid certificate found.\nEntity Id: {0}\nCertificate Type: {1}\nFederation Type: {2}\nReason: {3}\nDescription: {4}",
                certificate.getIssuerEntityId(),
                certificate.getCertificateUse(),
                certificate.getFederationEntityType(),
                certificateValidity.getException().get().getReason(),
                certificateValidity.getException().get().getMessage());

        LOG.warn(message);
    }

}
