package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.CertificateValidity;
import uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.dto.InvalidCertificateDto;
import uk.gov.ida.hub.config.truststore.TrustStoreForCertificateProvider;

import java.security.KeyStore;
import java.security.cert.CertPathValidatorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.CertificateValidityChecker.createNonOCSPCheckingCertificateValidityChecker;

@RunWith(MockitoJUnitRunner.class)
public class CertificateValidityCheckerTest {
    @Mock
    private TrustStoreForCertificateProvider trustStoreForCertProvider;

    @Mock
    private CertificateChainValidator certificateChainValidator;

    @Mock
    private KeyStore trustStore;

    private CertificateValidityChecker certificateValidityChecker;
    private CertificateDetails certificateDetails;

    @Before
    public void setUp() throws Exception {
        certificateDetails = new CertificateDetails("entityId", new SignatureVerificationCertificateBuilder().build(), FederationEntityType.HUB);
        certificateValidityChecker = createNonOCSPCheckingCertificateValidityChecker(trustStoreForCertProvider, certificateChainValidator);
    }

    @Test
    public void getsInvalidCertificates() throws Exception {
        String description = "Certificate invalid";
        CertPathValidatorException certPathValidatorException = new CertPathValidatorException(description);

        when(trustStoreForCertProvider.getTrustStoreFor(certificateDetails.getFederationEntityType())).thenReturn(trustStore);
        when(certificateChainValidator.validate(certificateDetails.getX509(), trustStore)).thenReturn(CertificateValidity.invalid(certPathValidatorException));

        ImmutableList<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(ImmutableList.of(certificateDetails));

        InvalidCertificateDto expected = new InvalidCertificateDto(certificateDetails.getIssuerId(), certPathValidatorException.getReason(), CertificateType.SIGNING, certificateDetails.getFederationEntityType(), description);

        assertThat(invalidCertificates).usingFieldByFieldElementComparator().containsOnly(expected);
    }

    @Test
    public void getsEmptyListWhenAllCertificatesAreValid() throws Exception {
        when(trustStoreForCertProvider.getTrustStoreFor(certificateDetails.getFederationEntityType())).thenReturn(trustStore);
        when(certificateChainValidator.validate(certificateDetails.getX509(), trustStore)).thenReturn(CertificateValidity.valid());

        ImmutableList<InvalidCertificateDto> invalidCertificates = certificateValidityChecker.getInvalidCertificates(ImmutableList.of(certificateDetails));

        assertThat(invalidCertificates).isEmpty();
    }

}