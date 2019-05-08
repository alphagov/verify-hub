package uk.gov.ida.hub.config.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityConfigDataToCertificateDtoTransformerTest {

    private EntityConfigDataToCertificateDtoTransformer transformer;
    private ImmutableSet<TransactionConfig> transactionConfigs;
    private ImmutableSet<MatchingServiceConfig> matchingServiceConfigs;
    private ImmutableSet<IdentityProviderConfig> identityProviderConfigs;
    private final String entityId = "ENTITY_ID";

    @Before
    public void setUp() throws Exception {
        transformer = new EntityConfigDataToCertificateDtoTransformer();
        identityProviderConfigs = ImmutableSet.of();
        transactionConfigs = ImmutableSet.of();
        matchingServiceConfigs = ImmutableSet.of();
    }

    @Test
    public void shouldIncludeTransactionSignatureCertificatesAndEncryptionCertificateInList() throws Exception {
        TransactionConfig transactionConfig = mock(TransactionConfig.class);
        transactionConfigs = ImmutableSet.of(transactionConfig);

        Certificate signatureVerificationCertificate = mock(Certificate.class);
        Certificate encryptionCertificate = mock(EncryptionCertificate.class);

        String encryptionX509 = "encryptionX509";
        String signatureX509 = "signatureX509";
        when(encryptionCertificate.getX509()).thenReturn(encryptionX509);
        when(signatureVerificationCertificate.getX509()).thenReturn(signatureX509);

        doReturn(encryptionCertificate).when(transactionConfig).getEncryptionCertificate();
        doReturn(ImmutableList.of(signatureVerificationCertificate)).when(transactionConfig).getSignatureVerificationCertificates();
        doReturn(entityId).when(transactionConfig).getEntityId();

        ImmutableList<CertificateDetails> certificates = transformer.transform(transactionConfigs, matchingServiceConfigs);

        assertThat(certificates.size()).isEqualTo(2);
        assertThat(certificates.get(0).getX509()).isEqualTo(signatureX509);
        assertThat(certificates.get(0).getIssuerId()).isEqualTo(entityId);
        assertThat(certificates.get(1).getX509()).isEqualTo(encryptionX509);
        assertThat(certificates.get(1).getIssuerId()).isEqualTo(entityId);
    }

    @Test
    public void shouldIncludeMatchingServiceSignatureCertificatesAndEncryptionCertificateInList() throws Exception {
        MatchingServiceConfig matchingServiceConfig = mock(MatchingServiceConfig.class);
        matchingServiceConfigs = ImmutableSet.of(matchingServiceConfig);

        Certificate signatureVerificationCertificate = mock(Certificate.class);
        Certificate encryptionCertificate = mock(EncryptionCertificate.class);

        String encryptionX509 = "encryptionX509";
        String signatureX509 = "signatureX509";
        when(encryptionCertificate.getX509()).thenReturn(encryptionX509);
        when(signatureVerificationCertificate.getX509()).thenReturn(signatureX509);

        doReturn(encryptionCertificate).when(matchingServiceConfig).getEncryptionCertificate();
        doReturn(ImmutableList.of(signatureVerificationCertificate)).when(matchingServiceConfig).getSignatureVerificationCertificates();
        doReturn(entityId).when(matchingServiceConfig).getEntityId();


        ImmutableList<CertificateDetails> entityCertificates = transformer.transform(transactionConfigs, matchingServiceConfigs);

        assertThat(entityCertificates.size()).isEqualTo(2);
        assertThat(entityCertificates.get(0).getX509()).isEqualTo(signatureX509);
        assertThat(entityCertificates.get(0).getIssuerId()).isEqualTo(entityId);
        assertThat(entityCertificates.get(1).getX509()).isEqualTo(encryptionX509);
        assertThat(entityCertificates.get(1).getIssuerId()).isEqualTo(entityId);

    }
}