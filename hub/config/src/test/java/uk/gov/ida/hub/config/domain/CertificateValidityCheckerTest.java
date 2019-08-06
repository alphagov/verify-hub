package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import java.security.KeyStore;
import java.security.cert.CertPathValidatorException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker;
import static uk.gov.ida.saml.core.test.PemCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

@RunWith(MockitoJUnitRunner.class)
public class CertificateValidityCheckerTest {
    @Mock
    private TrustStoreForCertificateProvider trustStoreForCertProvider;

    @Mock
    private CertificateChainValidator certificateChainValidator;

    @Mock
    private KeyStore trustStore;

    private CertificateValidityChecker certificateValidityChecker;
    private Certificate certificate;

    @Before
    public void setUp() {
        certificate = new Certificate("entityId", FederationEntityType.RP, HUB_TEST_PUBLIC_SIGNING_CERT, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);
        certificateValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertProvider, certificateChainValidator);
        when(trustStoreForCertProvider.getTrustStoreFor(certificate.getFederationEntityType())).thenReturn(trustStore);
    }

    @Test
    public void getsInvalidCertificates() {
        String description = "X509 Certificate is missing or badly formed.";
        CertPathValidatorException certPathValidatorException = new CertPathValidatorException(description);

        when(certificateChainValidator.validate(certificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(certPathValidatorException));

        Set<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(ImmutableList.of(certificate));

        InvalidCertificateDto expected = new InvalidCertificateDto(certificate.getIssuerEntityId(), certPathValidatorException.getReason(), CertificateUse.SIGNING, certificate.getFederationEntityType(), description);

        assertThat(invalidCertificates).usingFieldByFieldElementComparator().containsOnly(expected);
    }

    @Test
    public void getsEmptyListWhenAllCertificatesAreValid() {
        when(certificateChainValidator.validate(certificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());

        Set<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(ImmutableList.of(certificate));

        assertThat(invalidCertificates).isEmpty();
    }

    @Test
    public void validateReturnsCertificateValidityWhenValid() {
        when(certificateChainValidator.validate(certificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());
        Optional<CertificateValidity> result = certificateValidityChecker.validate(certificate);
        assertThat(result).isPresent();
        assertThat(result.get().isValid()).isTrue();
    }

    @Test
    public void validateReturnsCertificateValidityWhenNotValid() {
        CertPathValidatorException certPathValidatorException = new CertPathValidatorException("Bad Certificate chain");
        when(certificateChainValidator.validate(certificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(certPathValidatorException));
        Optional<CertificateValidity> result = certificateValidityChecker.validate(certificate);
        assertThat(result).isPresent();
        assertThat(result.get().isValid()).isFalse();
    }

    @Test
    public void determinesWhenSingleCertificateIsValid() {
        when(certificateChainValidator.validate(certificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());

        Boolean isCertificateValid = certificateValidityChecker.isValid(certificate);

        assertThat(isCertificateValid).isTrue();
    }

    @Test
    public void determinesWhenCertificateIsInValid() {
        when(certificateChainValidator.validate(certificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(new CertPathValidatorException()));

        Boolean isCertificateValid = certificateValidityChecker.isValid(certificate);

        assertThat(isCertificateValid).isFalse();
    }
}
