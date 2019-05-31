package uk.gov.ida.hub.config.application;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.hub.config.domain.CertificateDetails;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;
import uk.gov.ida.hub.config.domain.TransactionConfig;
import uk.gov.ida.hub.config.exceptions.NoCertificateFoundException;
import uk.gov.ida.saml.core.test.TestCertificateStrings;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CertificateServiceTest {
    @Mock
    private RoleBasedCertificateService<TransactionConfig> transactionCertificateService;

    @Mock
    private RoleBasedCertificateService<MatchingServiceConfig> matchingServiceCertificateService;

    private CertificateService certificateService;

    @Before
    public void createService() {
        certificateService = new CertificateService(matchingServiceCertificateService, transactionCertificateService);
    }

    @Test
    public void getAlllCertificateCertificateDetails_shouldReturnAllTransactionAndMatchingServiceCertificateDetails() {
        Set<CertificateDetails> transactionCertificateDetails = ImmutableSet.of(
            mock(CertificateDetails.class), mock(CertificateDetails.class), mock(CertificateDetails.class)
        );
        Set<CertificateDetails> matchingServiceCertificateDetails = ImmutableSet.of(
            mock(CertificateDetails.class), mock(CertificateDetails.class), mock(CertificateDetails.class)
        );
        when(transactionCertificateService.getAllCertificateDetails()).thenReturn(transactionCertificateDetails);
        when(matchingServiceCertificateService.getAllCertificateDetails()).thenReturn(matchingServiceCertificateDetails);
        Set<CertificateDetails> allCertificateDetails = certificateService.getAllCertificateDetails();
        assertThat(allCertificateDetails).containsAll(transactionCertificateDetails);
        assertThat(allCertificateDetails).containsAll(matchingServiceCertificateDetails);
        verify(transactionCertificateService).getAllCertificateDetails();
        verify(matchingServiceCertificateService).getAllCertificateDetails();
    }

    @Test
    public void signatureVerificationCertificatesFor_shouldTryFromTheTransactionsFirst() {
        List<CertificateDetails> transactionCertificateDetails = ImmutableList.of(
            mock(CertificateDetails.class), mock(CertificateDetails.class), mock(CertificateDetails.class)
        );
        when(transactionCertificateService.signatureVerificationCertificatesFor("FOO")).thenReturn(Optional.of(transactionCertificateDetails));
        assertThat(certificateService.signatureVerificationCertificatesFor("FOO")).containsAll(transactionCertificateDetails);
        verify(transactionCertificateService).signatureVerificationCertificatesFor("FOO");
        verify(matchingServiceCertificateService, never()).signatureVerificationCertificatesFor("FOO");
    }

    @Test
    public void signatureVerificationCertificatesFor_shouldTryMatchingServiceWhenNoneFoundInTransactions() {
        List<CertificateDetails> matchingServiceCertificateDetails = ImmutableList.of(
            mock(CertificateDetails.class), mock(CertificateDetails.class), mock(CertificateDetails.class)
        );
        when(transactionCertificateService.signatureVerificationCertificatesFor("FOO")).thenReturn(Optional.empty());
        when(matchingServiceCertificateService.signatureVerificationCertificatesFor("FOO")).thenReturn(Optional.of(matchingServiceCertificateDetails));
        assertThat(certificateService.signatureVerificationCertificatesFor("FOO")).containsAll(matchingServiceCertificateDetails);
        verify(transactionCertificateService).signatureVerificationCertificatesFor("FOO");
        verify(matchingServiceCertificateService).signatureVerificationCertificatesFor("FOO");
    }

    @Test
    public void signatureVerificationCertificatesFor_shouldErrorWhenNoCertificatesFound() {
        when(transactionCertificateService.signatureVerificationCertificatesFor("FOO")).thenReturn(Optional.empty());
        when(matchingServiceCertificateService.signatureVerificationCertificatesFor("FOO")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.signatureVerificationCertificatesFor("FOO"))
            .isInstanceOf(NoCertificateFoundException.class);
        verify(transactionCertificateService).signatureVerificationCertificatesFor("FOO");
        verify(matchingServiceCertificateService).signatureVerificationCertificatesFor("FOO");
    }

    @Test
    public void encryptionCertificateFor_shouldTryFromTheTransactionsFirst() {
        CertificateDetails encryptionCertificate = mock(CertificateDetails.class);
        when(transactionCertificateService.encryptionCertificateFor("FOO")).thenReturn(Optional.of(encryptionCertificate));
        assertThat(certificateService.encryptionCertificateFor("FOO")).isEqualTo(encryptionCertificate);
        verify(transactionCertificateService).encryptionCertificateFor("FOO");
        verify(matchingServiceCertificateService, never()).encryptionCertificateFor("FOO");
    }

    @Test
    public void encryptionCertificateFor_shouldTryMatchingServiceWhenNoneFoundInTransactions() {
        CertificateDetails encryptionCertificate = mock(CertificateDetails.class);
        when(transactionCertificateService.encryptionCertificateFor("FOO")).thenReturn(Optional.empty());
        when(matchingServiceCertificateService.encryptionCertificateFor("FOO")).thenReturn(Optional.of(encryptionCertificate));
        assertThat(certificateService.encryptionCertificateFor("FOO")).isEqualTo(encryptionCertificate);
        verify(transactionCertificateService).encryptionCertificateFor("FOO");
        verify(matchingServiceCertificateService).encryptionCertificateFor("FOO");
    }

    @Test
    public void encryptionCertificateFor_shouldErrorWhenNoCertificatesFound() {
        when(transactionCertificateService.encryptionCertificateFor("FOO")).thenReturn(Optional.empty());
        when(matchingServiceCertificateService.encryptionCertificateFor("FOO")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.encryptionCertificateFor("FOO"))
            .isInstanceOf(NoCertificateFoundException.class);
        verify(transactionCertificateService).encryptionCertificateFor("FOO");
        verify(matchingServiceCertificateService).encryptionCertificateFor("FOO");
    }
}
