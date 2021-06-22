package uk.gov.ida.hub.samlsoapproxy.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlsoapproxy.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlsoapproxy.domain.CertificateDto;
import uk.gov.ida.hub.samlsoapproxy.domain.FederationEntityType;
import uk.gov.ida.saml.core.test.TestEntityIds;

import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.ida.common.shared.security.verification.CertificateValidity.invalid;
import static uk.gov.ida.common.shared.security.verification.CertificateValidity.valid;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_SECONDARY_CERT;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceKeyStoreTest {

    public static final String IDP_ENTITY_ID_1 = TestEntityIds.STUB_IDP_ONE;
    public static final String IDP_ENTITY_ID_2 = TestEntityIds.STUB_IDP_TWO;
    public static final String RP_ENTITY_ID = TestEntityIds.TEST_RP;

    public static final String idpSigningCertPrimary = STUB_IDP_PUBLIC_PRIMARY_CERT;
    public static final String idpSigningCertSecondary = STUB_IDP_PUBLIC_SECONDARY_CERT;
    public static final String idpEncryptionCertPrimary = STUB_IDP_PUBLIC_PRIMARY_CERT;
    public static final String rpSigningCertPrimary = STUB_IDP_PUBLIC_PRIMARY_CERT;

    @Mock
    private CertificatesConfigProxy certificatesConfigProxy;
    @Mock
    private CertificateChainValidator certificateChainValidator;
    @Mock
    private X509CertificateFactory x509CertificateFactory;
    @Mock
    private TrustStoreForCertificateProvider trustStoreForCertificateProvider;
    @Mock
    private X509Certificate x509Certificate;
    @Mock
    private KeyStore trustStore;

    private String issuerId;
    private ConfigServiceKeyStore configServiceKeyStore;

    @Before
    public void setUp() {
        issuerId = "issuer-id";
        configServiceKeyStore = new ConfigServiceKeyStore(
                certificatesConfigProxy,
                certificateChainValidator,
                trustStoreForCertificateProvider,
                x509CertificateFactory);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldGetVerifyingKeysFromConfigCertificateProxy() {
        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(certificatesConfigProxy).getSignatureVerificationCertificates(issuerId);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldReturnAllKeysReturnedByConfig() {
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID_1, idpSigningCertPrimary);
        final CertificateDto certTwoDto = buildCertificateDto(IDP_ENTITY_ID_2, idpSigningCertSecondary);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(List.of(certOneDto, certTwoDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(x509CertificateFactory.createCertificate(certTwoDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.of(trustStore));
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        List<PublicKey> keys = configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        assertThat(keys.size()).isEqualTo(2);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldValidateEachKeyReturnedByConfig() {
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID_1, idpSigningCertPrimary);
        final CertificateDto certTwoDto = buildCertificateDto(IDP_ENTITY_ID_2, idpSigningCertSecondary);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(List.of(certOneDto, certTwoDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(x509CertificateFactory.createCertificate(certTwoDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.of(trustStore));
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(certificateChainValidator, times(2)).validate(x509Certificate, trustStore);
    }

    @Test
    public void getVerificationKeyForEntity_shouldThrowExceptionIfCertificateIsInvalid() {
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID_1, idpSigningCertPrimary);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(List.of(certOneDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.of(trustStore));
        CertPathValidatorException underlyingException = new CertPathValidatorException("Invalid Certificate");
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(invalid(underlyingException));
        try {
            configServiceKeyStore.getVerifyingKeysForEntity(issuerId);
            fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: Unable to get DN");
            assertThat(success.getCause()).isEqualTo(underlyingException);
        }
    }

    @Test
    public void getVerificationKeyForEntity_shouldNotValidateWhenTrustStoreDisabled() {
        final CertificateDto certOneDto = buildCertificateDto(RP_ENTITY_ID, rpSigningCertPrimary);
        when(certificatesConfigProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(List.of(certOneDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.empty());
        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);
        verify(certificateChainValidator, times(0)).validate(x509Certificate, trustStore);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldGetEncryptionKeysFromConfigCertificateProxy() {
        when(certificatesConfigProxy.getEncryptionCertificate(anyString())).thenReturn(buildCertificateDto(IDP_ENTITY_ID_1, idpEncryptionCertPrimary));
        when(x509CertificateFactory.createCertificate(anyString())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.of(trustStore));
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(certificatesConfigProxy).getEncryptionCertificate(issuerId);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldValidateTheKeyReturnedByConfig() {
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID_1, idpSigningCertPrimary);
        when(certificatesConfigProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.of(trustStore));
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(certificateChainValidator).validate(x509Certificate, trustStore);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldThrowExceptionIfCertificateIsInvalid() {
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID_1, idpSigningCertPrimary);
        when(certificatesConfigProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.of(trustStore));
        CertPathValidatorException underlyingException = new CertPathValidatorException("Invalid Certificate");
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(invalid(underlyingException));
        try {
            configServiceKeyStore.getEncryptionKeyForEntity(issuerId);
            fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: Unable to get DN");
            assertThat(success.getCause()).isEqualTo(underlyingException);
        }
    }

    @Test
    public void getEncryptionKeyForEntity_shouldNotValidateWhenTrustStoreDisabled() {
        final CertificateDto certOneDto = buildCertificateDto(RP_ENTITY_ID, rpSigningCertPrimary);
        when(certificatesConfigProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(Optional.empty());
        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);
        verify(certificateChainValidator, times(0)).validate(x509Certificate, trustStore);
    }

    private static CertificateDto buildCertificateDto(String entityId, String cert) {
        return new CertificateDtoBuilder().withIssuerId(entityId).withCertificate(cert).build();
    }
}
