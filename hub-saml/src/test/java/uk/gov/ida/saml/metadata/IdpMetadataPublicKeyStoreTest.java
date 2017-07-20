package uk.gov.ida.saml.metadata;

import com.google.common.base.Throwables;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder;
import uk.gov.ida.saml.metadata.exceptions.NoKeyConfiguredForEntityException;
import uk.gov.ida.saml.metadata.test.factories.metadata.EntityDescriptorFactory;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import static com.google.common.base.Throwables.propagate;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class IdpMetadataPublicKeyStoreTest {

    private static MetadataResolver metadataResolver;

    @BeforeClass
    public static void setUp() throws Exception {
        metadataResolver = initializeMetadata();
    }

    private static MetadataResolver initializeMetadata() throws URISyntaxException {
        try {
            EntityDescriptorFactory descriptorFactory = new EntityDescriptorFactory();
            String metadata = new MetadataFactory().metadata(asList(
                    descriptorFactory.hubEntityDescriptor(),
                    idpEntityDescriptor(TestEntityIds.STUB_IDP_ONE, TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT)
            ));
            InitializationService.initialize();
            StringBackedMetadataResolver stringBackedMetadataResolver = new StringBackedMetadataResolver(metadata);
            BasicParserPool basicParserPool = new BasicParserPool();
            basicParserPool.initialize();
            stringBackedMetadataResolver.setParserPool(basicParserPool);
            stringBackedMetadataResolver.setMinRefreshDelay(14400000);
            stringBackedMetadataResolver.setRequireValidMetadata(true);
            stringBackedMetadataResolver.setId("testResolver");
            stringBackedMetadataResolver.initialize();
            return stringBackedMetadataResolver;
        } catch (InitializationException | ComponentInitializationException e) {
            throw propagate(e);
        }
    }

    private static PublicKey getX509Key(String encodedCertificate) throws Base64DecodingException, CertificateException {
        byte[] derValue = Base64.decode(encodedCertificate);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(derValue));
        return certificate.getPublicKey();
    }

    private static EntityDescriptor idpEntityDescriptor(String idpEntityId, String public_signing_certificate) {
        KeyDescriptor keyDescriptor = buildKeyDescriptor(public_signing_certificate);
        IDPSSODescriptor idpssoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor().addKeyDescriptor(keyDescriptor).withoutDefaultSigningKey().build();
        try {
            return EntityDescriptorBuilder.anEntityDescriptor()
                    .withEntityId(idpEntityId)
                    .withIdpSsoDescriptor(idpssoDescriptor)
                    .withValidUntil(DateTime.now().plusWeeks(2))
                    .withSignature(null)
                    .withoutSigning()
                    .setAddDefaultSpServiceDescriptor(false)
                    .build();
        } catch (MarshallingException | SignatureException e) {
            throw Throwables.propagate(e);
        }
    }

    private static KeyDescriptor buildKeyDescriptor(String certificate) {
        X509Certificate x509Certificate = X509CertificateBuilder.aX509Certificate().withCert(certificate).build();
        X509Data build = X509DataBuilder.aX509Data().withX509Certificate(x509Certificate).build();
        KeyInfo signing_one = KeyInfoBuilder.aKeyInfo().withKeyName("signing_one").withX509Data(build).build();
        return KeyDescriptorBuilder.aKeyDescriptor().withKeyInfo(signing_one).build();
    }

    @Test
    public void shouldReturnTheSigningKeysForAnEntity() throws Exception {
        IdpMetadataPublicKeyStore idpMetadataPublicKeyStore = new IdpMetadataPublicKeyStore(metadataResolver);

        PublicKey expectedPublicKey = getX509Key(TestCertificateStrings.STUB_IDP_PUBLIC_PRIMARY_CERT);
        assertThat(idpMetadataPublicKeyStore.getVerifyingKeysForEntity(TestEntityIds.STUB_IDP_ONE)).containsExactly(expectedPublicKey);
    }

    @Test(expected = NoKeyConfiguredForEntityException.class)
    public void shouldRaiseAnExceptionWhenThereIsNoEntityDescriptor() throws Exception {
        IdpMetadataPublicKeyStore idpMetadataPublicKeyStore = new IdpMetadataPublicKeyStore(metadataResolver);
        idpMetadataPublicKeyStore.getVerifyingKeysForEntity("my-invented-entity-id");
    }

    @Test(expected = NoKeyConfiguredForEntityException.class)
    public void shouldRaiseAnExceptionWhenAttemptingToRetrieveAnSPSSOFromMetadata() throws Exception {
        IdpMetadataPublicKeyStore idpMetadataPublicKeyStore = new IdpMetadataPublicKeyStore(metadataResolver);
        idpMetadataPublicKeyStore.getVerifyingKeysForEntity("https://signin.service.gov.uk");
    }
}
