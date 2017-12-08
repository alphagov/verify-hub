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
    private ImmutableSet<TransactionConfigEntityData> transactionConfigEntityDatas;
    private ImmutableSet<MatchingServiceConfigEntityData> matchingServiceConfigEntityDatas;
    private ImmutableSet<IdentityProviderConfigEntityData> identityProviderConfigEntityDatas;
    private final String entityId = "ENTITY_ID";

    @Before
    public void setUp() throws Exception {
        transformer = new EntityConfigDataToCertificateDtoTransformer();
        identityProviderConfigEntityDatas = ImmutableSet.of();
        transactionConfigEntityDatas = ImmutableSet.of();
        matchingServiceConfigEntityDatas = ImmutableSet.of();
    }

    @Test
    public void shouldIncludeTransactionSignatureCertificatesAndEncryptionCertificateInList() throws Exception {
        TransactionConfigEntityData transactionConfigEntityData = mock(TransactionConfigEntityData.class);
        transactionConfigEntityDatas = ImmutableSet.of(transactionConfigEntityData);

        Certificate signatureVerificationCertificate = mock(Certificate.class);
        Certificate encryptionCertificate = mock(EncryptionCertificate.class);

        String encryptionX509 = "encryptionX509";
        String signatureX509 = "signatureX509";
        when(encryptionCertificate.getX509()).thenReturn(encryptionX509);
        when(signatureVerificationCertificate.getX509()).thenReturn(signatureX509);

        doReturn(encryptionCertificate).when(transactionConfigEntityData).getEncryptionCertificate();
        doReturn(ImmutableList.of(signatureVerificationCertificate)).when(transactionConfigEntityData).getSignatureVerificationCertificates();
        doReturn(entityId).when(transactionConfigEntityData).getEntityId();

        ImmutableList<CertificateDetails> certificates = transformer.transform(transactionConfigEntityDatas, matchingServiceConfigEntityDatas);

        assertThat(certificates.size()).isEqualTo(2);
        assertThat(certificates.get(0).getX509()).isEqualTo(signatureX509);
        assertThat(certificates.get(0).getIssuerId()).isEqualTo(entityId);
        assertThat(certificates.get(1).getX509()).isEqualTo(encryptionX509);
        assertThat(certificates.get(1).getIssuerId()).isEqualTo(entityId);
    }

    @Test
    public void shouldIncludeMatchingServiceSignatureCertificatesAndEncryptionCertificateInList() throws Exception {
        MatchingServiceConfigEntityData matchingServiceConfigEntityData = mock(MatchingServiceConfigEntityData.class);
        matchingServiceConfigEntityDatas = ImmutableSet.of(matchingServiceConfigEntityData);

        Certificate signatureVerificationCertificate = mock(Certificate.class);
        Certificate encryptionCertificate = mock(EncryptionCertificate.class);

        String encryptionX509 = "encryptionX509";
        String signatureX509 = "signatureX509";
        when(encryptionCertificate.getX509()).thenReturn(encryptionX509);
        when(signatureVerificationCertificate.getX509()).thenReturn(signatureX509);

        doReturn(encryptionCertificate).when(matchingServiceConfigEntityData).getEncryptionCertificate();
        doReturn(ImmutableList.of(signatureVerificationCertificate)).when(matchingServiceConfigEntityData).getSignatureVerificationCertificates();
        doReturn(entityId).when(matchingServiceConfigEntityData).getEntityId();


        ImmutableList<CertificateDetails> entityCertificates = transformer.transform(transactionConfigEntityDatas, matchingServiceConfigEntityDatas);

        assertThat(entityCertificates.size()).isEqualTo(2);
        assertThat(entityCertificates.get(0).getX509()).isEqualTo(signatureX509);
        assertThat(entityCertificates.get(0).getIssuerId()).isEqualTo(entityId);
        assertThat(entityCertificates.get(1).getX509()).isEqualTo(encryptionX509);
        assertThat(entityCertificates.get(1).getIssuerId()).isEqualTo(entityId);

    }
}