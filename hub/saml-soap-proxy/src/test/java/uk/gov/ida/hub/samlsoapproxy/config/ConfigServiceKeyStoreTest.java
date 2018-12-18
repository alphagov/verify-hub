package uk.gov.ida.hub.samlsoapproxy.config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlsoapproxy.builders.CertificateDtoBuilder;
import uk.gov.ida.hub.samlsoapproxy.contract.MatchingServiceConfigEntityDataDto;
import uk.gov.ida.hub.samlsoapproxy.domain.CertificateDto;
import uk.gov.ida.hub.samlsoapproxy.domain.FederationEntityType;

import java.io.IOException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.ida.common.shared.security.verification.CertificateValidity.invalid;
import static uk.gov.ida.common.shared.security.verification.CertificateValidity.valid;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.STUB_IDP_PUBLIC_SECONDARY_CERT;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceKeyStoreTest {

    public static final String IDP_ENTITY_ID = "http://stub-idp-one.local/SSO/POST";
    public static final String SECOND_IDP_ENTITY_ID = "http://stub-idp-two.local/SSO/POST";
    public static final String idpSigningCertPrimary = STUB_IDP_PUBLIC_PRIMARY_CERT;
    public static final String idpSigningCertSecondary = STUB_IDP_PUBLIC_SECONDARY_CERT;
    public static final String idpEncryptionCertPrimary = STUB_IDP_PUBLIC_PRIMARY_CERT;
    public static final HashMap<String, String> PUBLIC_SIGNING_CERTS = new HashMap<>();

    @Mock
    private ConfigProxy configProxy;
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
    @Mock
    private MatchingServiceAdapterMetadataRetriever matchingServiceAdapterMetadataRetriever;

    private String issuerId;
    private ConfigServiceKeyStore configServiceKeyStore;

    @Before
    public void setup() throws CertificateException {
        issuerId = "issuer-id";
        configServiceKeyStore = new ConfigServiceKeyStore(
                configProxy,
                certificateChainValidator,
                trustStoreForCertificateProvider,
                x509CertificateFactory,
                matchingServiceAdapterMetadataRetriever);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldGetVerifyingKeysFromConfigCertificateProxy() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());

        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(configProxy).getSignatureVerificationCertificates(issuerId);
        verify(matchingServiceAdapterMetadataRetriever, times(0)).getPublicSigningKeysForMSA(issuerId);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldReturnAllKeysReturnedByConfig() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID, idpSigningCertPrimary);
        final CertificateDto certTwoDto = buildCertificateDto(SECOND_IDP_ENTITY_ID, idpSigningCertSecondary);
        when(configProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(of(certOneDto, certTwoDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(x509CertificateFactory.createCertificate(certTwoDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        List<PublicKey> keys = configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        assertThat(keys.size()).isEqualTo(2);
    }

    @Test
    public void getVerifyingKeysForEntity_shouldValidateEachKeyReturnedByConfig() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID, idpSigningCertPrimary);
        final CertificateDto certTwoDto = buildCertificateDto(SECOND_IDP_ENTITY_ID, idpSigningCertSecondary);
        when(configProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(of(certOneDto, certTwoDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(x509CertificateFactory.createCertificate(certTwoDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(certificateChainValidator, times(2)).validate(x509Certificate, trustStore);
    }

    @Test
    public void getVerificationKeyForEntity_shouldThrowExceptionIfCertificateIsInvalid() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID, idpSigningCertPrimary);
        when(configProxy.getSignatureVerificationCertificates(issuerId)).thenReturn(of(certOneDto));
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        CertPathValidatorException underlyingException = new CertPathValidatorException("Invalid Certificate");
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(invalid(underlyingException));
        try {
            configServiceKeyStore.getVerifyingKeysForEntity(issuerId);
            Assert.fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: Unable to get DN");
            assertThat(success.getCause()).isEqualTo(underlyingException);
        }
    }

    @Test
    public void getEncryptionKeyForEntity_shouldGetEncryptionKeysFromConfigCertificateProxy() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());
        when(configProxy.getEncryptionCertificate(anyString())).thenReturn(buildCertificateDto(IDP_ENTITY_ID, idpEncryptionCertPrimary));
        when(x509CertificateFactory.createCertificate(anyString())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(configProxy).getEncryptionCertificate(issuerId);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldValidateTheKeyReturnedByConfig() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID, idpSigningCertPrimary);
        when(configProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(valid());

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(certificateChainValidator).validate(x509Certificate, trustStore);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldThrowExceptionIfCertificateIsInvalid() throws Exception {
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.empty());
        final CertificateDto certOneDto = buildCertificateDto(IDP_ENTITY_ID, idpSigningCertPrimary);
        when(configProxy.getEncryptionCertificate(issuerId)).thenReturn(certOneDto);
        when(x509CertificateFactory.createCertificate(certOneDto.getCertificate())).thenReturn(x509Certificate);
        when(trustStoreForCertificateProvider.getTrustStoreFor(any(FederationEntityType.class))).thenReturn(trustStore);
        CertPathValidatorException underlyingException = new CertPathValidatorException("Invalid Certificate");
        when(certificateChainValidator.validate(x509Certificate, trustStore)).thenReturn(invalid(underlyingException));
        try {
            configServiceKeyStore.getEncryptionKeyForEntity(issuerId);
            Assert.fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: Unable to get DN");
            assertThat(success.getCause()).isEqualTo(underlyingException);
        }
    }

    @Test
    public void getSigningKeyForEntity_shouldGetCertFromMetadataForMSAWhenIndicatedByConfig() throws Exception {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = mock(MatchingServiceConfigEntityDataDto.class);
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.ofNullable(matchingServiceConfigEntityDataDto));
        when(matchingServiceConfigEntityDataDto.getReadMetadataFromEntityId()).thenReturn(true);

        configServiceKeyStore.getVerifyingKeysForEntity(issuerId);

        verify(matchingServiceAdapterMetadataRetriever, times(1)).getPublicSigningKeysForMSA(issuerId);
    }

    @Test
    public void getEncryptionKeyForEntity_shouldGetCertFromMetadataForMSAWhenIndicatedByConfig() throws Exception {
        MatchingServiceConfigEntityDataDto matchingServiceConfigEntityDataDto = mock(MatchingServiceConfigEntityDataDto.class);
        when(configProxy.getMsaConfiguration(issuerId)).thenReturn(Optional.ofNullable(matchingServiceConfigEntityDataDto));
        when(matchingServiceConfigEntityDataDto.getReadMetadataFromEntityId()).thenReturn(true);

        configServiceKeyStore.getEncryptionKeyForEntity(issuerId);

        verify(matchingServiceAdapterMetadataRetriever, times(1)).getPublicEncryptionKeyForMSA(issuerId);
    }

    private static CertificateDto buildCertificateDto(String entityId, String cert) throws IOException {
        return new CertificateDtoBuilder().withIssuerId(entityId).withCertificate(cert).build();
    }
}
