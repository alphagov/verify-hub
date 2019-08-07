package uk.gov.ida.hub.config.domain;

import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.common.shared.security.verification.OCSPCertificateChainValidator;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import java.security.cert.CertPathValidatorException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CertificateValidityChecker {
    public static CertificateValidityChecker createOCSPCheckingCertificateValidityChecker(TrustStoreForCertificateProvider trustStoreForCertificateProvider, OCSPCertificateChainValidator ocspCertificateChainValidator) {
        return new CertificateValidityChecker(trustStoreForCertificateProvider, ocspCertificateChainValidator);
    }

    public static CertificateValidityChecker createNonOCSPCheckingCertificateValidityChecker(TrustStoreForCertificateProvider trustStoreForCertificateProvider, CertificateChainValidator certificateChainValidator) {
        return new CertificateValidityChecker(trustStoreForCertificateProvider, certificateChainValidator);
    }

    private final TrustStoreForCertificateProvider trustStoreForCertificateProvider;
    private final CertificateChainValidator certificateChainValidator;

    private CertificateValidityChecker(TrustStoreForCertificateProvider trustStoreForCertificateProvider, CertificateChainValidator certificateChainValidator) {
        this.trustStoreForCertificateProvider = trustStoreForCertificateProvider;
        this.certificateChainValidator = certificateChainValidator;
    }

    public Set<InvalidCertificateDto> getInvalidCertificates(Collection<Certificate> certificates) {
        return certificates.stream()
                .map(cert -> new CertAndValidityPair(cert, getCertificateValidity(cert).orElse(createNoX509CertificateValidity())))
                .filter(p -> !p.certificateValidity.isValid())
                .map(this::toInvalidCertificates)
                .collect(Collectors.toSet());
    }

    public Optional<CertificateValidity> validate(Certificate certificate) {
        return certificate.getX509Certificate()
                .map(x509 -> certificateChainValidator.validate(
                        x509,
                        trustStoreForCertificateProvider.getTrustStoreFor(certificate.getFederationEntityType())));
    }

    public boolean isValid(Certificate certificate) {
        return getCertificateValidity(certificate)
                .map(CertificateValidity::isValid)
                .orElse(false);
    }

    private Optional<CertificateValidity> getCertificateValidity(Certificate certificate){
        return certificate.getX509Certificate()
                .map(x509 -> certificateChainValidator.validate(
                        x509,
                        trustStoreForCertificateProvider.getTrustStoreFor(certificate.getFederationEntityType())));
    }

    private InvalidCertificateDto toInvalidCertificates(CertAndValidityPair cavPair) {
        CertPathValidatorException certPathValidatorException = cavPair.certificateValidity.getException().get();
        return new InvalidCertificateDto(
                cavPair.certificate.getIssuerEntityId(),
                certPathValidatorException.getReason(),
                cavPair.certificate.getCertificateUse(),
                cavPair.certificate.getFederationEntityType(),
                certPathValidatorException.getMessage());
    }

    private CertificateValidity createNoX509CertificateValidity(){
        return CertificateValidity.invalid(new CertPathValidatorException("X509 Certificate is missing or badly formed."));
    }

    private class CertAndValidityPair {
        Certificate certificate;
        CertificateValidity certificateValidity;

        CertAndValidityPair(Certificate certificate, CertificateValidity certificateValidity){
            this.certificate = certificate;
            this.certificateValidity = certificateValidity;
        }
    }
}
