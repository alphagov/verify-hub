package uk.gov.ida.hub.config.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.ida.hub.config.data.ManagedEntityConfigRepository;
import uk.gov.ida.hub.config.domain.Certificate;
import uk.gov.ida.hub.config.domain.CertificateOrigin;
import uk.gov.ida.hub.config.domain.CertificateUse;
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


@ExtendWith(MockitoExtension.class)
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
    private ListAppender<ILoggingEvent> logsListAppender = null;

    @BeforeEach
    public void createService() {
        certificateService = new CertificateService(connectedServiceConfigRepository, matchingServiceConfigRepository, certificateValidityChecker);

        Logger certificateServiceLogger = (Logger) LoggerFactory.getLogger(CertificateService.class);
        logsListAppender = new ListAppender<>();
        logsListAppender.start();
        certificateServiceLogger.addAppender(logsListAppender);
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
                CERT_ONE_X509, CertificateUse.ENCRYPTION, CertificateOrigin.FEDERATION, true));
    }

    @Test
    public void encryptionCertificateForEntityIdWarnsAndThrowsWhenTransactionCertificateExistsButIsInvalid() {
        Assertions.assertThrows(NoCertificateFoundException.class, () -> {
            TransactionConfig transactionConfig = aTransactionConfigData()
                    .withEntityId(RP_ONE_ENTITY_ID)
                    .withEnabled(true)
                    .build();

            when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
            when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
            when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(false);

            try {
                certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
            }
            finally {
                String expectedLogMessage = "Encryption certificate for entityId '" + RP_ONE_ENTITY_ID + "' was requested but is invalid";
                checkForExpectedLogWarnings(List.of(expectedLogMessage));
            }
        });
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
                CERT_ONE_X509, CertificateUse.ENCRYPTION, CertificateOrigin.FEDERATION, true));
    }

    @Test
    public void encryptionCertificateForEntityIdWarnsAndThrowsWhenMatchCertificateExistsButIsInvalid() {
        Assertions.assertThrows(NoCertificateFoundException.class, () -> {
            MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                    .withEntityId(RP_ONE_ENTITY_ID)
                    .build();

            when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
            when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
            when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(false);

            try {
                certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
            }
            finally {
                String expectedLogMessage = "Encryption certificate for entityId '" + RP_ONE_ENTITY_ID + "' was requested but is invalid";
                checkForExpectedLogWarnings(List.of(expectedLogMessage));
            }
        });
    }


    @Test
    public void encryptionCertificateForEntityIdThrowsWhenNoEncryptionCertificateExists() {
        Assertions.assertThrows(NoCertificateFoundException.class, () -> {
            when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
            when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);

            certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
        });
    }

    @Test
    public void encryptionCertificateForEntityIdThrowsWhenEncryptionCertificateExistsButIsNotEnabled() {
        Assertions.assertThrows(CertificateDisabledException.class, () -> {
            TransactionConfig transactionConfig = aTransactionConfigData()
                    .withEntityId(RP_ONE_ENTITY_ID)
                    .withEnabled(false)
                    .build();

            when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
            when(connectedServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(transactionConfig));
            when(certificateValidityChecker.isValid(any(Certificate.class))).thenReturn(true);

            certificateService.encryptionCertificateFor(RP_ONE_ENTITY_ID);
        });

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
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_ONE_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true),
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_TWO_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true)
        );
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsValidSignatureVerificationCertificatesWhenTransactionSignatureCertificatesExist() {

        TransactionConfig transactionConfig = aTransactionConfigData()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_ONE_X509)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        Certificate validCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_ONE_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);
        Certificate invalidCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_TWO_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);

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
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_ONE_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true),
                new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.RP, CERT_TWO_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true)
        );
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdReturnsValidSignatureVerificationCertificatesWhenMatchingSignatureCertificatesExist() {

        MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                .withEntityId(RP_ONE_ENTITY_ID)
                .addSignatureVerificationCertificate(CERT_ONE_X509)
                .addSignatureVerificationCertificate(CERT_TWO_X509)
                .build();

        Certificate validCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_ONE_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);
        Certificate invalidCertificate = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_TWO_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);

        when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
        when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
        when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<Certificate> CertificateFound = certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);

        assertThat(CertificateFound.size()).isEqualTo(1);
        assertThat(CertificateFound.get(0)).isEqualTo(validCertificate);

        String expectedLogMessage = String.format("Signature verification certificates were requested for entityId '%s'; 1 of them is invalid", RP_ONE_ENTITY_ID);
        checkForExpectedLogWarnings(List.of(expectedLogMessage));
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdWarnsAndThrowsWhenMatchingSignatureCertificatesExistButAreInvalid() {
        Assertions.assertThrows(NoCertificateFoundException.class, () -> {
            MatchingServiceConfig matchingServiceConfig = aMatchingServiceConfig()
                    .withEntityId(RP_ONE_ENTITY_ID)
                    .addSignatureVerificationCertificate(CERT_ONE_X509)
                    .addSignatureVerificationCertificate(CERT_TWO_X509)
                    .build();

            Certificate invalidCertificate1 = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_ONE_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);
            Certificate invalidCertificate2 = new Certificate(RP_ONE_ENTITY_ID, FederationEntityType.MS, CERT_TWO_X509, CertificateUse.SIGNING, CertificateOrigin.FEDERATION, true);

            when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
            when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(true);
            when(matchingServiceConfigRepository.get(RP_ONE_ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfig));
            when(certificateValidityChecker.isValid(invalidCertificate1)).thenReturn(false);
            when(certificateValidityChecker.isValid(invalidCertificate2)).thenReturn(false);

            try {
                certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);
            }
            finally {
                String expectedLogMessage = String.format("Signature verification certificates were requested for entityId '%s'; 2 of them are invalid", RP_ONE_ENTITY_ID);
                checkForExpectedLogWarnings(List.of(expectedLogMessage));
            }
        });
    }

    @Test
    public void signatureVerificationCertificatesForEntityIdThrowsWhenMatchingSignatureCertificatesDoNotExist() {
        Assertions.assertThrows(NoCertificateFoundException.class, () -> {
            when(connectedServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);
            when(matchingServiceConfigRepository.has(RP_ONE_ENTITY_ID)).thenReturn(false);

            certificateService.signatureVerificationCertificatesFor(RP_ONE_ENTITY_ID);
        });
    }

    @Test
    public void getAllCertificatesReturnsAllTransactionAndMatchingServiceCertificate() {
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

    private void checkForExpectedLogWarnings(Iterable<String> expectedMessages) {
        for (String expectedMessage : expectedMessages) {
            assertThat(logsListAppender.list.stream()
                    .anyMatch(logEvent ->
                            logEvent.getFormattedMessage().contentEquals(expectedMessage) && logEvent.getLevel() == Level.WARN)
            ).isTrue();
        }

    }

}
