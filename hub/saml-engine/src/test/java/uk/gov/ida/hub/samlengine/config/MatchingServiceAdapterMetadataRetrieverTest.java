package uk.gov.ida.hub.samlengine.config;

import certificates.values.CACertificates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import keystore.CertificateEntry;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.common.shared.security.verification.CertificateChainValidator;
import uk.gov.ida.common.shared.security.verification.PKIXParametersProvider;
import uk.gov.ida.common.shared.security.verification.exceptions.CertificateChainValidationException;
import uk.gov.ida.hub.samlengine.domain.FederationEntityType;
import uk.gov.ida.hub.samlengine.exceptions.EncryptionKeyExtractionException;
import uk.gov.ida.hub.samlengine.exceptions.SigningKeyExtractionException;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.metadata.factories.DropwizardMetadataResolverFactory;

import java.security.PublicKey;
import java.security.cert.CertPathValidatorException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_ENCRYPTION_CERT;
import static uk.gov.ida.saml.core.test.TestCertificateStrings.TEST_RP_MS_PUBLIC_SIGNING_CERT;

@RunWith(OpenSAMLMockitoRunner.class)
public class MatchingServiceAdapterMetadataRetrieverTest {

    private MSAStubRule msaStubRule;
    private String entityId;
    private static final PublicKey msaPublicSigningKey = new X509CertificateFactory().createCertificate(TEST_RP_MS_PUBLIC_SIGNING_CERT).getPublicKey();
    private static final PublicKey msaPublicEncKey = new X509CertificateFactory().createCertificate(TEST_RP_MS_PUBLIC_ENCRYPTION_CERT).getPublicKey();

    @Mock
    private TrustStoreForCertificateProvider trustStoreForCertificateProvider;

    private static KeyStoreResource msTrustStore;
    private MatchingServiceAdapterMetadataRetriever matchingServiceAdapterMetadataRetriever;

    @BeforeClass
    public static void setupResolver() {
        msTrustStore = KeyStoreResourceBuilder.aKeyStoreResource()
                .withCertificates(ImmutableList.of(new CertificateEntry("test_root_ca", CACertificates.TEST_ROOT_CA),
                        new CertificateEntry("test_rp_ca", CACertificates.TEST_RP_CA)))
                .build();
        msTrustStore.create();
    }

    @Before
    public void setup() {
        msaStubRule = new MSAStubRule();
        entityId = msaStubRule.METADATA_ENTITY_ID;
        when(trustStoreForCertificateProvider.getTrustStoreFor(FederationEntityType.MS)).thenReturn(msTrustStore.getKeyStore());
        matchingServiceAdapterMetadataRetriever = new MatchingServiceAdapterMetadataRetriever(trustStoreForCertificateProvider, new CertificateChainValidator(new PKIXParametersProvider(), new X509CertificateFactory()), new DropwizardMetadataResolverFactory());
    }

    @Test(expected = SigningKeyExtractionException.class)
    public void getSigningKey_cannotRetreiveMetadata() throws JsonProcessingException {
        msaStubRule.setUpMissingMetadata();
        matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
    }

    @Test(expected = EncryptionKeyExtractionException.class)
    public void getEncryptionKey_cannotRetreiveMetadata() throws JsonProcessingException {
        msaStubRule.setUpMissingMetadata();
        matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForMSA(entityId);
    }

    @Test(expected = SigningKeyExtractionException.class)
    public void getSigningKey_noCertsInMetadata() throws JsonProcessingException, MarshallingException, SignatureException {
        msaStubRule.setUpMetadataWithoutCerts();
        matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
    }

    @Test(expected = EncryptionKeyExtractionException.class)
    public void getEncryptionKey_noCertsInMetadata() throws JsonProcessingException, MarshallingException, SignatureException {
        msaStubRule.setUpMetadataWithoutCerts();
        matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForMSA(entityId);
    }

    @Test
    public void getSigningKey_doesNotReturnBadCertsInMetadata() throws JsonProcessingException, MarshallingException, SignatureException {
        msaStubRule.setUpMetadataWithContainingCertsFromAnotherCertChain();
        try {
            matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
            fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: CN=IDA Hub Signing Dev, OU=GDS, O=Cabinet Office, L=London, ST=London, C=GB");
            assertThat(success.getCause()).isInstanceOf(CertPathValidatorException.class);
        }
    }

    @Test
    public void getEncryptionKey_doesNotReturnBadCertsInMetadata() throws JsonProcessingException, MarshallingException, SignatureException {
        msaStubRule.setUpMetadataWithContainingCertsFromAnotherCertChain();
        try {
            matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForMSA(entityId);
            fail(String.format("Expected [%s]", CertificateChainValidationException.class.getSimpleName()));
        } catch (CertificateChainValidationException success) {
            assertThat(success.getMessage()).isEqualTo("Certificate is not valid: CN=IDA Hub Encryption Dev, OU=GDS, O=Cabinet Office, L=London, ST=London, C=GB");
            assertThat(success.getCause()).isInstanceOf(CertPathValidatorException.class);
        }
    }

    @Test
    public void getSigningKeysForEntity_shouldGetKeysFromMetadata() throws Exception {
        msaStubRule.setUpRegularMetadata();
        final List<PublicKey> publicSigningKeysForMSA = matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
        assertThat(publicSigningKeysForMSA.size()).isEqualTo(1);
        assertThat(publicSigningKeysForMSA.get(0)).isEqualTo(msaPublicSigningKey);
    }

    @Test
    public void getEncryptionKeysForEntity_shouldGetKeysFromMetadata() throws Exception {
        msaStubRule.setUpRegularMetadata();
        final PublicKey publicEncryptionKeyForMSA = matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForMSA(entityId);
        assertThat(publicEncryptionKeyForMSA).isNotNull();
        assertThat(publicEncryptionKeyForMSA).isEqualTo(msaPublicEncKey);
    }

    @Test
    @Ignore
    public void getSigningKeysForEntity_shouldGetMultipleKeysFromMetadataWhereAppropriate() throws Exception {
        msaStubRule.setUpRegularMetadataWithTwoSigningCerts();
        final List<PublicKey> publicSigningKeysForMSA = matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
        assertThat(publicSigningKeysForMSA.size()).isEqualTo(2);
        assertThat(publicSigningKeysForMSA.get(0)).isEqualTo(msaPublicSigningKey);
    }

    @Test(expected = SigningKeyExtractionException.class)
    public void getSigningKey_badlySignedMetadata() throws JsonProcessingException, MarshallingException, SignatureException {
        msaStubRule.setUpBadlySignedMetadata();
        matchingServiceAdapterMetadataRetriever.getPublicSigningKeysForMSA(entityId);
    }

    @Test(expected = EncryptionKeyExtractionException.class)
    public void getEncryptionKey_badlySignedMetadata() throws JsonProcessingException, MarshallingException, SignatureException {
        msaStubRule.setUpBadlySignedMetadata();
        matchingServiceAdapterMetadataRetriever.getPublicEncryptionKeyForMSA(entityId);
    }

}
