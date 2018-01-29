package uk.gov.ida.hub.config.domain;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.common.shared.security.verification.OCSPCertificateChainValidator;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import java.security.cert.CertPathValidatorException;
import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.getOnlyElement;

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

    public ImmutableList<InvalidCertificateDto> getInvalidCertificates(Collection<CertificateDetails> certificateDetails) {
        FluentIterable<InvalidCertificateDto> invalidCertificates = from(certificateDetails)
                .transform(toDetailsValidityMap())
                .filter(invalidCertificateValidities())
                .transform(toInvalidCertificates());

        return copyOf(invalidCertificates);
    }

    public boolean isValid(CertificateDetails certificate) {
        CertificateValidity certificateValidity = certificateChainValidator.validate(
                certificate.getX509(),
                trustStoreForCertificateProvider.getTrustStoreFor(certificate.getFederationEntityType()));

        return certificateValidity.isValid();
    }

    private Function<Map<CertificateDetails, CertificateValidity>, InvalidCertificateDto> toInvalidCertificates() {
        return input -> {
            CertificateDetails certificateDetail = getOnlyElement(input.keySet());
            CertificateValidity certificateValidity = getOnlyElement(input.values());

            CertPathValidatorException certPathValidatorException = certificateValidity.getException().get();
            return new InvalidCertificateDto(
                    certificateDetail.getIssuerId(),
                    certPathValidatorException.getReason(),
                    certificateDetail.getKeyUse(),
                    certificateDetail.getFederationEntityType(),
                    certPathValidatorException.getMessage());
        };
    }

    private Predicate<Map<CertificateDetails, CertificateValidity>> invalidCertificateValidities() {
        return input -> !getOnlyElement(input.values()).isValid();
    }

    private Function<CertificateDetails, Map<CertificateDetails, CertificateValidity>> toDetailsValidityMap() {
        return input -> {
            CertificateValidity certificateValidity = certificateChainValidator.validate(
                    input.getX509(),
                    trustStoreForCertificateProvider.getTrustStoreFor(input.getFederationEntityType()));
            return ImmutableMap.of(input, certificateValidity);
        };
    }
}
