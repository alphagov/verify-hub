package uk.gov.ida.hub.config.domain;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
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
    private Certificate localCertificate;
    private Certificate selfServiceCertificate;

    @Before
    public void setUp() {
        localCertificate = new Certificate("entityId", FederationEntityType.RP, HUB_TEST_PUBLIC_SIGNING_CERT, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);
        selfServiceCertificate = new Certificate("entityId", FederationEntityType.RP, HUB_TEST_PUBLIC_SIGNING_CERT, CertificateUse.SIGNING, CertificateOrigin.SELFSERVICE, true);
        certificateValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertProvider, certificateChainValidator);
        when(trustStoreForCertProvider.getTrustStoreFor(localCertificate.getFederationEntityType())).thenReturn(trustStore);
    }

    @Test
    public void getsInvalidCertificates() {
        String description = "X509 Certificate is missing or badly formed.";
        CertPathValidatorException certPathValidatorException = new CertPathValidatorException(description);

        when(certificateChainValidator.validate(localCertificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(certPathValidatorException));

        Set<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(List.of(localCertificate));

        InvalidCertificateDto expected = new InvalidCertificateDto(localCertificate.getIssuerEntityId(), certPathValidatorException.getReason(), CertificateUse.SIGNING, localCertificate.getFederationEntityType(), description);

        assertThat(invalidCertificates).usingFieldByFieldElementComparator().containsOnly(expected);
    }

    @Test
    public void getsEmptyListWhenAllCertificatesAreValid() {
        when(certificateChainValidator.validate(localCertificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());

        Set<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(List.of(localCertificate));

        assertThat(invalidCertificates).isEmpty();
    }

    @Test
    public void validateReturnsCertificateValidityForValidLocalCert() {
        when(certificateChainValidator.validate(localCertificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());
        Optional<CertificateValidity> result = certificateValidityChecker.validate(localCertificate);
        assertThat(result).isPresent();
        assertThat(result.get().isValid()).isTrue();
    }

    @Test
    public void validateReturnsCertificateValidityForInvalidLocalCert() {
        CertPathValidatorException certPathValidatorException = new CertPathValidatorException("Bad Certificate chain");
        when(certificateChainValidator.validate(localCertificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(certPathValidatorException));
        Optional<CertificateValidity> result = certificateValidityChecker.validate(localCertificate);
        assertThat(result).isPresent();
        assertThat(result.get().isValid()).isFalse();
    }

    @Test
    public void validateReturnsNoResultForLocalCertWithEmptyX509() {
        Certificate certificateWithoutX509 = new Certificate("entityId", FederationEntityType.RP, "", CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);
        Optional<CertificateValidity> result = certificateValidityChecker.validate(certificateWithoutX509);
        assertThat(result).isEmpty();
    }

    @Test
    public void validateConsidersSelfServiceCertToBeValidWithoutCheckingTrustChain() {
        Optional<CertificateValidity> result = certificateValidityChecker.validate(selfServiceCertificate);
        assertThat(result).isPresent();
        assertThat(result.get().isValid()).isTrue();
        verifyZeroInteractions(certificateChainValidator);
    }

    @Test
    public void validateReturnsNoResultForSelfServiceCertWithEmptyX509() {
        Certificate certificateWithoutX509 = new Certificate("entityId", FederationEntityType.RP, "", CertificateUse.SIGNING, CertificateOrigin.SELFSERVICE, true);
        Optional<CertificateValidity> result = certificateValidityChecker.validate(certificateWithoutX509);
        assertThat(result).isEmpty();
    }

    @Test
    public void determinesWhenLocalCertificateIsValid() {
        when(certificateChainValidator.validate(localCertificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.valid());

        Boolean isCertificateValid = certificateValidityChecker.isValid(localCertificate);

        assertThat(isCertificateValid).isTrue();
    }

    @Test
    public void determinesWhenLocalCertificateIsInvalid() {
        when(certificateChainValidator.validate(localCertificate.getX509Certificate().get(), trustStore)).thenReturn(CertificateValidity.invalid(new CertPathValidatorException()));

        Boolean isCertificateValid = certificateValidityChecker.isValid(localCertificate);

        assertThat(isCertificateValid).isFalse();
    }

    @Test
    public void considersSelfServiceCertificateToBeValidWithoutCheckingTrustChain() {
        Boolean isCertificateValid = certificateValidityChecker.isValid(selfServiceCertificate);

        assertThat(isCertificateValid).isTrue();

        verifyZeroInteractions(certificateChainValidator);
    }

    @Test
    public void considersLocalCertificateWithEmptyX509ToBeInvalid() {
        Certificate certificateWithoutX509 = new Certificate("entityId", FederationEntityType.RP, "", CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);

        Boolean isCertificateValid = certificateValidityChecker.isValid(certificateWithoutX509);

        assertThat(isCertificateValid).isFalse();
    }

    @Test
    public void considersSelfServiceCertificateWithEmptyX509ToBeInvalid() {
        Certificate certificateWithoutX509 = new Certificate("entityId", FederationEntityType.RP, "", CertificateUse.SIGNING, CertificateOrigin.SELFSERVICE, true);

        Boolean isCertificateValid = certificateValidityChecker.isValid(certificateWithoutX509);

        assertThat(isCertificateValid).isFalse();
    }
}
