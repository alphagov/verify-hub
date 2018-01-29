package uk.gov.ida.hub.config.application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.hub.config.data.ConfigEntityDataRepository;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.MatchingServiceConfigEntityData;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;
import uk.gov.ida.hub.config.dto.FederationEntityType;
import uk.gov.ida.hub.config.exceptions.CertificateDisabledException;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.ida.hub.config.domain.builders.MatchingServiceConfigEntityDataBuilder.aMatchingServiceConfigEntityData;
import static uk.gov.ida.hub.config.domain.builders.SignatureVerificationCertificateBuilder.aSignatureVerificationCertificate;
import static uk.gov.ida.hub.config.domain.builders.TransactionConfigEntityDataBuilder.aTransactionConfigData;

@RunWith(MockitoJUnitRunner.class)
public class CertificateServiceTest {

    @Mock
    private ConfigEntityDataRepository<TransactionConfigEntityData> transactionDataSource;

    @Mock
    private ConfigEntityDataRepository<MatchingServiceConfigEntityData> matchingServiceDataSource;


    private CertificateService certificateService;

    @Before
    public void createService() {
        certificateService = new CertificateService(transactionDataSource, matchingServiceDataSource);
    }

    @Test
    public void findsEncryptionCertificate_WhenEnabledTransCertificateExists() throws Exception {
        String entityId = "id_that_exists";
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(entityId)
                .withEnabled(true)
                .build();

        when(matchingServiceDataSource.getData(entityId)).thenReturn(Optional.empty());
        when(transactionDataSource.getData(entityId)).thenReturn(Optional.of(transactionConfigEntityData));

        CertificateDetails certificateDetails = certificateService.encryptionCertificateFor(entityId);

        assertThat(certificateDetails.getIssuerId()).isEqualTo(entityId);
        assertThat(certificateDetails.getX509()).isEqualTo(transactionConfigEntityData.getEncryptionCertificate().getX509());
        assertThat(certificateDetails.getFederationEntityType()).isEqualTo(FederationEntityType.RP);
    }

    @Test
    public void findsEncryptionCertificate_WhenMatchingCertificateExists() throws Exception {
        String entityId = "id_that_exists";
        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(entityId)
                .build();

        when(transactionDataSource.getData(entityId)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(entityId)).thenReturn(Optional.of(matchingServiceConfigEntityData));

        CertificateDetails certificateDetails = certificateService.encryptionCertificateFor(entityId);

        assertThat(certificateDetails.getIssuerId()).isEqualTo(entityId);
        assertThat(certificateDetails.getX509()).isEqualTo(matchingServiceConfigEntityData.getEncryptionCertificate().getX509());
        assertThat(certificateDetails.getFederationEntityType()).isEqualTo(FederationEntityType.MS);
    }


    @Test(expected = NoCertificateFoundException.class)
    public void throwsNotFoundException_WhenNoEncryptionCertificateExists() throws Exception {
        String entityId = "id_that_exists";

        when(transactionDataSource.getData(entityId)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(entityId)).thenReturn(Optional.empty());

        certificateService.encryptionCertificateFor(entityId);
    }

    @Test(expected = CertificateDisabledException.class)
    public void throwsDisabledException_WhenEncryptionCertificateExistsButIsNotEnabled() throws Exception {
        String entityId = "id_that_exists";
        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(entityId)
                .withEnabled(false)
                .build();

        when(transactionDataSource.getData(entityId)).thenReturn(Optional.of(transactionConfigEntityData));

        certificateService.encryptionCertificateFor(entityId);
    }

    @Test
    public void findsSignatureVerificationCertificates_WhenTransactionSignatureCertificatesExists() throws Exception {
        String entityId = "id_that_exists";
        String certOneX509 = "cert1";
        String certTwoX509 = "cert2";

        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(certOneX509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(certTwoX509).build();

        TransactionConfigEntityData transactionConfigEntityData = aTransactionConfigData()
                .withEntityId(entityId)
                .addSignatureVerificationCertificate(sigCert1)
                .addSignatureVerificationCertificate(sigCert2)
                .build();

        when(matchingServiceDataSource.getData(entityId)).thenReturn(Optional.empty());
        when(transactionDataSource.getData(entityId)).thenReturn(Optional.of(transactionConfigEntityData));

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificatonCertificatesFor(entityId);

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound.stream().map(detail -> detail.getIssuerId()).collect(toSet())).contains(entityId);
        assertThat(certificateDetailsFound.stream().map(detail -> detail.getFederationEntityType()).collect(toSet())).contains(FederationEntityType.RP);
        assertThat(certificateDetailsFound.stream().map(detail -> detail.getX509()).collect(toList())).contains(certOneX509, certTwoX509);
    }

    @Test
    public void findsSignatureVerificationCertificates_WhenMatchingSignatureCertificatesExists() throws Exception {
        String entityId = "id_that_exists";
        String certOneX509 = "cert1";
        String certTwoX509 = "cert2";

        SignatureVerificationCertificate sigCert1 = aSignatureVerificationCertificate().withX509(certOneX509).build();
        SignatureVerificationCertificate sigCert2 = aSignatureVerificationCertificate().withX509(certTwoX509).build();

        MatchingServiceConfigEntityData matchingServiceConfigEntityData = aMatchingServiceConfigEntityData()
                .withEntityId(entityId)
                .addSignatureVerificationCertificate(sigCert1)
                .addSignatureVerificationCertificate(sigCert2)
                .build();

        when(transactionDataSource.getData(entityId)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(entityId)).thenReturn(Optional.of(matchingServiceConfigEntityData));

        List<CertificateDetails> certificateDetailsFound = certificateService.signatureVerificatonCertificatesFor(entityId);

        assertThat(certificateDetailsFound.size()).isEqualTo(2);
        assertThat(certificateDetailsFound.stream().map(detail -> detail.getIssuerId()).collect(toSet())).contains(entityId);
        assertThat(certificateDetailsFound.stream().map(detail -> detail.getFederationEntityType()).collect(toSet())).contains(FederationEntityType.MS);
        assertThat(certificateDetailsFound.stream().map(detail -> detail.getX509()).collect(toList())).contains(certOneX509, certTwoX509);
    }

    @Test(expected = NoCertificateFoundException.class)
    public void throwsNoCertificateFoundException_WhenMatchingSignatureCertificatesDoNotExist() throws Exception {
        String entityId = "id_that_exists";

        when(transactionDataSource.getData(entityId)).thenReturn(Optional.empty());
        when(matchingServiceDataSource.getData(entityId)).thenReturn(Optional.empty());

        certificateService.signatureVerificatonCertificatesFor(entityId);
    }
}
