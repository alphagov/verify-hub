package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigRepository;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.CertificateConfigurable;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
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
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigBuilder.aMatchingServiceConfig;
import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;
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
        List<ConfigRepository> configRepositories = new ArrayList<>();
        configRepositories.add(connectedServiceConfigRepository);
        configRepositories.add(matchingServiceConfigRepository);
        certificateService = new CertificateService(configRepositories, certificateValidityChecker);
    }

    @Test
    public void encryptionCertificateForEntityIdReturnsCertificateDetailsWhenEnabledTransactionCertificateExists() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .withEnabled(true)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        CertificateDetails certificateDetails = certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetails).isEqualTo(new CertificateDetails(RP_ONE_ENTITY_ID,
                transactionConfig.getEncryptionCertificate(), FederationEntityType.RP));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void encryptionCertificateForEntityIdThrowsWhenTransactionCertificateExistsButIsInvalid() {
        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .withEnabled(true)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(false);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }

    @Test
    public void encryptionCertificateForEntityIdReturnsCertificateDetailsWhenEnabledMatchingCertificateExists() {
        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        CertificateDetails certificateDetails = certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetails).isEqualTo(new CertificateDetails(RP_ONE_ENTITY_ID,
                matchingServiceConfig.getEncryptionCertificate(), FederationEntityType.MS));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void encryptionCertificateForEntityIdThrowsWhenMatchCertificateExistsButIsInvalid() {
        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .build();

        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(false);

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
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsSignatureVerificationCertificatesWhenTransactionSignatureCertificatesExists() {
        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(sigCert1)
                .addSignatureVerificationCertificate(sigCert2)
                .build();

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound).contains(new CertificateDetails(RP_ONE_ENTITY_ID, sigCert1, FederationEntityType.RP),
                new CertificateDetails(RP_ONE_ENTITY_ID, sigCert2, FederationEntityType.RP));
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsValidSignatureVerificationCertificatesWhenTransactionSignatureCertificatesExist() {
        SignatureVerificationCertificate validCert = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate invalidCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(validCert)
                .addSignatureVerificationCertificate(invalidCert)
                .build();

        CertificateDetails validCertificate = new CertificateDetails(RP_ONE_ENTITY_ID, validCert, FederationEntityType.RP);
        CertificateDetails invalidCertificate = new CertificateDetails(RP_ONE_ENTITY_ID, invalidCert, FederationEntityType.RP);

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(1);
        assertThat(certificateDetailsFound.get(0)).isEqualTo(validCertificate);
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsSignatureVerificationCertificatesWhenMatchingSignatureCertificatesExists() {
        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(sigCert1)
                .addSignatureVerificationCertificate(sigCert2)
                .build();

        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound).contains(new CertificateDetails(RP_ONE_ENTITY_ID, sigCert1, FederationEntityType.MS),
                new CertificateDetails(RP_ONE_ENTITY_ID, sigCert2, FederationEntityType.MS));
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsValidSignatureVerificationCertificatesWhenMatchingSignatureCertificatesExist() {
        SignatureVerificationCertificate validSigCert = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate invalidSigCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(validSigCert)
                .addSignatureVerificationCertificate(invalidSigCert)
                .build();

        CertificateDetails validCertificate = new CertificateDetails(RP_ONE_ENTITY_ID, validSigCert, FederationEntityType.MS);
        CertificateDetails invalidCertificate = new CertificateDetails(RP_ONE_ENTITY_ID, invalidSigCert, FederationEntityType.MS);

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(1);
        assertThat(certificateDetailsFound.get(0)).isEqualTo(validCertificate);
    }

    @Test(expected = NoCertificateFoundException.class)
    public void signatureVerificationCertificatesForEntityIdThrowsWhenMatchingSignatureCertificatesExistButAreInvalid() {
        SignatureVerificationCertificate invalidSigCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(invalidSigCert)
                .build();

        CertificateDetails invalidCertificate = new CertificateDetails(RP_ONE_ENTITY_ID, invalidSigCert, FederationEntityType.MS);

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
    public void getAllCertificateDetailsReturnsAllTransactionAndMatchingServiceCertificateDetails() {
        final TransactionConfig transactionOneConfig = aTransactionConfigData().withEntityId(RP_ONE_ENTITY_ID)
                .withEnabled(true)
                .build();
        final TransactionConfig transactionTwoConfig = aTransactionConfigData().withEntityId(RP_TWO_ENTITY_ID)
                .withEnabled(true)
                .build();
        final MatchingServiceConfig matchingServiceOneConfig = aMatchingServiceConfig().withEntityId(RP_MSA_ONE_ENTITY_ID)
                .build();
        Set<CertificateDetails> expectedCertificateDetailsSet = new HashSet<>();
        expectedCertificateDetailsSet.addAll(getCertificateDetailsSet(transactionOneConfig));
        expectedCertificateDetailsSet.addAll(getCertificateDetailsSet(transactionTwoConfig));
        expectedCertificateDetailsSet.addAll(getCertificateDetailsSet(matchingServiceOneConfig));

        List<TransactionConfig> transactionConfigs = new ArrayList<>();
        transactionConfigs.add(transactionOneConfig);
        transactionConfigs.add(transactionTwoConfig);
        List<MatchingServiceConfig> matchingServiceConfigs = new ArrayList<>();
        matchingServiceConfigs.add(matchingServiceOneConfig);
        when(connectedServiceConfigRepository.getAll()).thenReturn(transactionConfigs);
        when(matchingServiceConfigRepository.getAll()).thenReturn(matchingServiceConfigs);

        final Set<CertificateDetails> actualCertificateDetailsSet = certificateService.getAllCertificateDetails();

        assertThat(actualCertificateDetailsSet.size()).isEqualTo(6);
        assertThat(actualCertificateDetailsSet).containsAll(expectedCertificateDetailsSet);
    }

    private <T extends CertificateConfigurable<?>> Set<CertificateDetails>
    getCertificateDetailsSet(final T configEntityData) {
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
