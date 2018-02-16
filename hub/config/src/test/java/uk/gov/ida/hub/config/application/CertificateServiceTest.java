package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.CertificateValidityChecker;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.CertificateDetails.aCertificateDetail;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class CertificateServiceTest {

    private static final String ENTITY_ID = "id_that_exists";
    private static final String CERT_ONE_X509 = "cert1";
    private static final String CERT_TWO_X509 = "cert2";
    
    @Mock
    private ConfigEntityDataRepository<TransactionConfigEntityData> transactionDataSource;

    @Mock
    private ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceDataSource;

    @Mock
    private CertificateValidityChecker certificateValidityChecker;

    private CertificateService certificateService;

    @Before
    public void createService() {
        certificateService = new CertificateService(transactionDataSource, matchingServiceDataSource, certificateValidityChecker);
    }

    @Test
    public void findsEncryptionCertificate_WhenEnabledTransCertificateExists() throws Exception {
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(ENTITY_ID)
                .withEnabled(true)
                .build();

        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(transactionConfigEntityData));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        CertificateDetails certificateDetails = certificateService.encryptionCertificateFor(ENTITY_ID);

        assertThat(certificateDetails).isEqualTo(aCertificateDetail(ENTITY_ID,
                transactionConfigEntityData.getEncryptionCertificate(), FederationEntityType.RP));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void throwsNotFoundException_WhenEncryptionCertificateExistsButIsInvalid() throws Exception {
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(ENTITY_ID)
                .withEnabled(true)
                .build();

        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(transactionConfigEntityData));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(false);

        certificateService.encryptionCertificateFor(ENTITY_ID);
    }

    @Test
    public void findsEncryptionCertificate_WhenMatchingCertificateExists() throws Exception {
        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(ENTITY_ID)
                .build();

        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntityData));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        CertificateDetails certificateDetails = certificateService.encryptionCertificateFor(ENTITY_ID);

        assertThat(certificateDetails).isEqualTo(aCertificateDetail(ENTITY_ID,
                matchingServiceConfigEntityData.getEncryptionCertificate(), FederationEntityType.MS));
    }

    @Test(expected = NoCertificateFoundException.class)
    public void throwsNotFoundException_WhenMatchingCertificateExistsButIsInvalid() throws Exception {
        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(ENTITY_ID)
                .build();

        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntityData));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(false);

        certificateService.encryptionCertificateFor(ENTITY_ID);
    }


    @Test(expected = NoCertificateFoundException.class)
    public void throwsNotFoundException_WhenNoEncryptionCertificateExists() throws Exception {
        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());

        certificateService.encryptionCertificateFor(ENTITY_ID);
    }

    @Test(expected = CertificateDisabledException.class)
    public void throwsDisabledException_WhenEncryptionCertificateExistsButIsNotEnabled() throws Exception {
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(ENTITY_ID)
                .withEnabled(false)
                .build();

        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(transactionConfigEntityData));
        when(certificateValidityChecker.isValid(Mockito.any(CertificateDetails.class))).thenReturn(true);

        certificateService.encryptionCertificateFor(ENTITY_ID);
    }

    @Test
    public void findsSignatureVerificationCertificates_WhenTransactionSignatureCertificatesExists() throws Exception {
        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(ENTITY_ID)
                .addSignatureVerificationCertificate(sigCert1)
                .addSignatureVerificationCertificate(sigCert2)
                .build();

        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(transactionConfigEntityData));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificatonCertificatesFor(ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound).contains(aCertificateDetail(ENTITY_ID, sigCert1, FederationEntityType.RP),
                aCertificateDetail(ENTITY_ID, sigCert2, FederationEntityType.RP));
    }

    @Test
    public void findsOnlyValidSignatureVerificationCertificates_WhenTransactionSignatureCertificatesExists() throws Exception {
        SignatureVerificationCertificate validCert = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate invalidCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(ENTITY_ID)
                .addSignatureVerificationCertificate(validCert)
                .addSignatureVerificationCertificate(invalidCert)
                .build();

        CertificateDetails validCertificate = aCertificateDetail(ENTITY_ID, validCert, FederationEntityType.RP);
        CertificateDetails invalidCertificate = aCertificateDetail(ENTITY_ID, invalidCert, FederationEntityType.RP);

        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(transactionConfigEntityData));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificatonCertificatesFor(ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(1);
        assertThat(certificateDetailsFound.get(0)).isEqualTo(validCertificate);
    }

    @Test
    public void findsSignatureVerificationCertificates_WhenMatchingSignatureCertificatesExists() throws Exception {
        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(ENTITY_ID)
                .addSignatureVerificationCertificate(sigCert1)
                .addSignatureVerificationCertificate(sigCert2)
                .build();

        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntityData));
        when(certificateValidityChecker.isValid(any(CertificateDetails.class))).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificatonCertificatesFor(ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound).contains(aCertificateDetail(ENTITY_ID, sigCert1, FederationEntityType.MS),
                aCertificateDetail(ENTITY_ID, sigCert2, FederationEntityType.MS));
    }

    @Test
    public void findsOnlyValidSignatureVerificationCertificates_WhenMatchingSignatureCertificatesExists() throws Exception {
        SignatureVerificationCertificate validSigCert = aSignatureVerificationCertificate().withX509(CERT_ONE_X509).build();
        SignatureVerificationCertificate invalidSigCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(ENTITY_ID)
                .addSignatureVerificationCertificate(validSigCert)
                .addSignatureVerificationCertificate(invalidSigCert)
                .build();

        CertificateDetails validCertificate = new CertificateDetails(ENTITY_ID, validSigCert, FederationEntityType.MS);
        CertificateDetails invalidCertificate = new CertificateDetails(ENTITY_ID, invalidSigCert, FederationEntityType.MS);

        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntityData));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);
        when(certificateValidityChecker.isValid(validCertificate)).thenReturn(true);

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificatonCertificatesFor(ENTITY_ID);

        assertThat(certificateDetailsFound.size()).isEqualTo(1);
        assertThat(certificateDetailsFound.get(0)).isEqualTo(validCertificate);
    }

    @Test(expected = NoCertificateFoundException.class)
    public void throwsNoCertificateFoundException_WhenMatchingSignatureCertificatesExistButAreInvalid() throws Exception {
        SignatureVerificationCertificate invalidSigCert = aSignatureVerificationCertificate().withX509(CERT_TWO_X509).build();

        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(ENTITY_ID)
                .addSignatureVerificationCertificate(invalidSigCert)
                .build();

        CertificateDetails invalidCertificate = new CertificateDetails(ENTITY_ID, invalidSigCert, FederationEntityType.MS);

        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.of(matchingServiceConfigEntityData));
        when(certificateValidityChecker.isValid(invalidCertificate)).thenReturn(false);

        certificateService.signatureVerificatonCertificatesFor(ENTITY_ID);
    }

    @Test(expected = NoCertificateFoundException.class)
    public void throwsNoCertificateFoundException_WhenMatchingSignatureCertificatesDoNotExist() throws Exception {
        when(transactionDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(ENTITY_ID)).thenReturn(Optional.empty());

        certificateService.signatureVerificatonCertificatesFor(ENTITY_ID);
    }
}
