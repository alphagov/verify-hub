package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;
import uk.gov.ida.saml.core.test.TestCertificateStrings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.CertificateDetails.aCertifcateDetail;
import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class RoleBasedCertificateServiceTest {

    private static final String RP_ONE_ENTITY_ID = "rp_one_entity_id";
    private static final String RP_TWO_ENTITY_ID = "rp_two_entity_id";
    private static final String CERT_ONE_X509 = TestCertificateStrings.UNCHAINED_PUBLIC_CERT;
    private static final String CERT_TWO_X509 = TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

    @Mock
    private ManagedEntityConfigRepository<TransactionConfig> transactionConfigRepository;

    @Mock
    private CertificateValidityChecker certificateValidityChecker;

    private RoleBasedCertificateService<TransactionConfig> certificateService;

    @Before
    public void createService() {
        certificateService = new RoleBasedCertificateService<>(transactionConfigRepository, certificateValidityChecker);
    }

    @Test
    public void findsEncryptionCertificate_WhenEnabledTransCertificateExists() {
        TransactionConfig transactionConfig = aTransactionConfigData()
            .withEntityId(RP_ONE_ENTITY_ID)
            .withEnabled(true)
            .build();

        when(transactionConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        Optional<CertificateDetails> certificateDetails = certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetails.get()).isEqualTo(aCertifcateDetail(RP_ONE_ENTITY_ID,
            transactionConfig.getEncryptionCertificate(), FederationEntityType.RP));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void throwsNotFoundException_WhenEncryptionCertificateExistsButIsInvalid() {
        TransactionConfig transactionConfig = aTransactionConfigData()
            .withEntityId(RP_ONE_ENTITY_ID)
            .withEnabled(true)
            .build();

        when(transactionConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(false);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }


    public void returnEmpty_WhenEntityCantBeFound() {
        when(transactionConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.empty());

        assertThat(certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID).isEmpty()).isTrue();
    }

    @Test(expected = CertificateDisabledException.class)
    public void throwsDisabledException_WhenEncryptionCertificateExistsButIsNotEnabled() {
        TransactionConfig transactionConfig = aTransactionConfigData()
            .withEntityId(RP_ONE_ENTITY_ID)
            .withEnabled(false)
            .build();

        when(transactionConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }

    @Test
    public void findsSignatureVerificationCertificates_WhenTransactionSignatureCertificatesExists() {
        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        TransactionConfig transactionConfig = aTransactionConfigData()
            .withEntityId(RP_ONE_ENTITY_ID)
            .addSignatureVerificationCertificate(sigCert1)
            .addSignatureVerificationCertificate(sigCert2)
            .build();

        when(transactionConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID).get();

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound).contains(aCertifcateDetail(RP_ONE_ENTITY_ID, sigCert1, FederationEntityType.RP),
            aCertifcateDetail(RP_ONE_ENTITY_ID, sigCert2, FederationEntityType.RP));
    }

    @Test
    public void findsOnlyValidSignatureVerificationCertificates_WhenTransactionSignatureCertificatesExists() {
        SignatureVerificationCertificate validCert = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate invalidCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        TransactionConfig transactionConfig = aTransactionConfigData()
            .withEntityId(RP_ONE_ENTITY_ID)
            .addSignatureVerificationCertificate(validCert)
            .addSignatureVerificationCertificate(invalidCert)
            .build();

        CertificateDetails validCertificate = aCertifcateDetail(RP_ONE_ENTITY_ID, validCert, FederationEntityType.RP);
        CertificateDetails invalidCertificate = aCertifcateDetail(RP_ONE_ENTITY_ID, invalidCert, FederationEntityType.RP);

        when(transactionConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID).get();

        assertThat(certificateDetailsFound.size()).isEqualTo(1);
        assertThat(certificateDetailsFound.get(0)).isEqualTo(validCertificate);
    }

    @Test
    public void getsAllCertificatesMetrics() {
        final TransactionConfig transactionOneConfig = aTransactionConfigData().withEntityId(RP_ONE_ENTITY_ID)
            .withEnabled(true)
            .build();
        final TransactionConfig transactionTwoConfig = aTransactionConfigData().withEntityId(RP_TWO_ENTITY_ID)
            .withEnabled(true)
            .build();
        Set<CertificateDetails> expectedCertificateDetailsSet = new HashSet<>();
        expectedCertificateDetailsSet.addAll(getCertificateDetailsSet(transactionOneConfig));
        expectedCertificateDetailsSet.addAll(getCertificateDetailsSet(transactionTwoConfig));

        List<TransactionConfig> transactionConfigs = new ArrayList<>();
        transactionConfigs.add(transactionOneConfig);
        transactionConfigs.add(transactionTwoConfig);
        when(transactionConfigRepository.getAll()).thenReturn(transactionConfigs);

        final Set<CertificateDetails> actualCertificateDetailsSet = certificateService.getAllCertificateDetails();

        assertThat(actualCertificateDetailsSet.size()).isEqualTo(4);
        assertThat(actualCertificateDetailsSet).containsAll(expectedCertificateDetailsSet);
    }

    private Set<CertificateDetails> getCertificateDetailsSet(final TransactionConfig configEntityData) {
        Set<CertificateDetails> certificateDetailsSet = new HashSet<>();
        configEntityData.getSignatureVerificationCertificates()
            .forEach(certificate -> certificateDetailsSet.add(new CertificateDetails(
                configEntityData.getEntityId(),
                certificate,
                configEntityData.getEntityType(),
                configEntityData.isEnabled())));
        certificateDetailsSet.add(new CertificateDetails(
            configEntityData.getEntityId(),
            configEntityData.getEncryptionCertificate(),
            configEntityData.getEntityType(),
            configEntityData.isEnabled()));
        return certificateDetailsSet;
    }

}
