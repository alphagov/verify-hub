package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateType;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
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
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigBuilder.aTransactionConfigData;


@RunWith(MockitoJUnitRunner.class)
public class CertificateServiceTest {

    private static final String RP_ONE_ENTITY_ID = "rp_one_entity_id";
    private static final String RP_TWO_ENTITY_ID = "rp_two_entity_id";
    private static final String RP_MSA_ONE_ENTITY_ID = "rp_msa_one_entity_id";
    private static final String CERT_ONE_X509 = TestCertificateStrings.UNCHAINED_PUBLIC_CERT;
    private static final String CERT_TWO_X509 = TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT;

    @Mock
    private ManagedEntityConfigRepository<TransactionConfig> connectedServiceConfigRepository;

    @Mock
    private ManagedEntityConfigRepository<MatchingServiceConfig> matchingServiceConfigRepository;

    @Mock
    private CertificateValidityChecker certificateValidityChecker;


    private CertificateService certificateService;

    @Before
    public void createService() {
        certificateService = new CertificateService(connectedServiceConfigRepository, matchingServiceConfigRepository, certificateValidityChecker);
    }

    @Test
    public void encryptionCertificateForEntityIdReturnsCertificateWhenEnabledTransactionCertificateExists() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .withEncryptionCertificate(CERT_ONE_X509)
                .withEnabled(true)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(true);

        Certificate certificate = certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);

        assertThat(certificate).isEqualTo(new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP,
                CERT_ONE_X509, CertificateType.ENCRYPTION, true));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void encryptionCertificateForEntityIdThrowsWhenTransactionCertificateExistsButIsInvalid() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .withEnabled(true)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(false);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }

    @Test
    public void encryptionCertificateForEntityIdReturnsCertificateWhenEnabledMatchingCertificateExists() {
        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .withEncryptionCertificate(CERT_ONE_X509)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(true);

        Certificate certificate = certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);

        assertThat(certificate).isEqualTo(new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP,
                CERT_ONE_X509, CertificateType.ENCRYPTION, true));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void encryptionCertificateForEntityIdThrowsWhenMatchCertificateExistsButIsInvalid() {
        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .build();

        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(false);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }


    @Test(expected = NoCertificateFoundException.class)
    public void encryptionCertificateForEntityIdThrowsWhenNoEncryptionCertificateExists() {
        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }

    @Test(expected = CertificateDisabledException.class)
    public void encryptionCertificateForEntityIdThrowsWhenEncryptionCertificateExistsButIsNotEnabled() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .withEnabled(false)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(true);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsSignatureVerificationCertificatesWhenTransactionSignatureCertificatesExists() {

        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_ONE_X509)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(true);

        List<Certificate> CertificateFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(CertificateFound.size()).isEqualTo(2);
        assertThat(CertificateFound).contains(
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_ONE_X509, CertificateType.SIGNING, true),
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_TWO_X509, CertificateType.SIGNING, true)
        );
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsValidSignatureVerificationCertificatesWhenTransactionSignatureCertificatesExist() {

        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_ONE_X509)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        Certificate validCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_ONE_X509, CertificateType.SIGNING, true);
        Certificate invalidCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_TWO_X509, CertificateType.SIGNING, true);

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<Certificate> CertificateFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(CertificateFound.size()).isEqualTo(1);
        assertThat(CertificateFound.get(0)).isEqualTo(validCertificate);
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsSignatureVerificationCertificatesWhenMatchingSignatureCertificatesExists() {

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_ONE_X509)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(true);

        List<Certificate> CertificateFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(CertificateFound.size()).isEqualTo(2);
        assertThat(CertificateFound).contains(
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_ONE_X509, CertificateType.SIGNING, true),
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_TWO_X509, CertificateType.SIGNING, true)
        );
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsValidSignatureVerificationCertificatesWhenMatchingSignatureCertificatesExist() {

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_ONE_X509)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        Certificate validCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_ONE_X509, CertificateType.SIGNING, true);
        Certificate invalidCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_TWO_X509, CertificateType.SIGNING, true);

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<Certificate> CertificateFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(CertificateFound.size()).isEqualTo(1);
        assertThat(CertificateFound.get(0)).isEqualTo(validCertificate);
    }

    @Test(expected = NoCertificateFoundException.class)
    public void signatureVerificationCertificatesForEntityIdThrowsWhenMatchingSignatureCertificatesExistButAreInvalid() {

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        Certificate invalidCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_TWO_X509, CertificateType.SIGNING, true);

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);

        certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);
    }

    @Test(expected = NoCertificateFoundException.class)
    public void signatureVerificationCertificatesForEntityIdThrowsWhenMatchingSignatureCertificatesDoNotExist() {
        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);

        certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);
    }

    @Test
    public void getAllCertificateReturnsAllTransactionAndMatchingServiceCertificate() {
        final TransactionConfig transactionOneConfig = aTransactionConfigData().withEntityId(RP_ONE_ENTITY_ID)
                .withEnabled(true)
                .build();
        final TransactionConfig transactionTwoConfig = aTransactionConfigData().withEntityId(RP_TWO_ENTITY_ID)
                .withEnabled(true)
                .build();
        final MatchingServiceConfig matchingServiceOneConfig = aMatchingServiceConfig().withEntityId(RP_MSA_ONE_ENTITY_ID)
                .build();
        Set<Certificate> expectedCertificateSet = new HashSet<>();
        expectedCertificateSet.addAll(transactionOneConfig.getAllCertificates());
        expectedCertificateSet.addAll(transactionTwoConfig.getAllCertificates());
        expectedCertificateSet.addAll(matchingServiceOneConfig.getAllCertificates());

        List<TransactionConfig> transactionConfigs = new ArrayList<>();
        transactionConfigs.add(transactionOneConfig);
        transactionConfigs.add(transactionTwoConfig);
        List<MatchingServiceConfig> matchingServiceConfigs = new ArrayList<>();
        matchingServiceConfigs.add(matchingServiceOneConfig);
        when(connectedServiceConfigRepository.stream()).thenReturn(transactionConfigs.stream());
        when(matchingServiceConfigRepository.stream()).thenReturn(matchingServiceConfigs.stream());

        final Set<Certificate> actualCertificateSet = certificateService.getAllCertificates();

        assertThat(actualCertificateSet.size()).isEqualTo(6);
        assertThat(actualCertificateSet).containsAll(expectedCertificateSet);
    }

}