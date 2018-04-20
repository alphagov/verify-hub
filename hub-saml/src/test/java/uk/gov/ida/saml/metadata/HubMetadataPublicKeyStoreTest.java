package uk.gov.ida.saml.metadata;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.impl.AbstractReloadingMetadataResolver;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.metadata.test.factories.metadata.MetadataFactory;
import uk.gov.ida.saml.metadata.exceptions.HubEntityMissingException;
import uk.gov.ida.saml.security.PublicKeyFactory;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class HubMetadataPublicKeyStoreTest {
    private static MetadataResolver metadataResolver;
    private static MetadataResolver invalidMetadataResolver;
    private static MetadataResolver emptyMetadataResolver;

    @BeforeClass
    public static void setUp() throws Exception {
        MetadataFactory metadataFactory = new MetadataFactory();
        metadataResolver = initializeMetadata(metadataFactory.defaultMetadata());
        invalidMetadataResolver = initializeMetadata(metadataFactory.expiredMetadata());
        emptyMetadataResolver = initializeMetadata(metadataFactory.emptyMetadata());
    }

    private static MetadataResolver initializeMetadata(String xml) throws URISyntaxException, ComponentInitializationException {
        AbstractReloadingMetadataResolver metadataResolver = new StringBackedMetadataResolver(xml);
        BasicParserPool basicParserPool = new BasicParserPool();
        basicParserPool.initialize();
        metadataResolver.setParserPool(basicParserPool);
        metadataResolver.setId("testResolver");
        metadataResolver.setRequireValidMetadata(true);
        metadataResolver.initialize();
        return metadataResolver;
    }

    private static PublicKey getX509Key(String encodedCertificate) throws Base64DecodingException, CertificateException {
        byte[] derValue = Base64.decode(encodedCertificate);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(derValue));
        return certificate.getPublicKey();
    }

    @Test
    public void shouldReturnTheSigningPublicKeysForTheHub() throws Exception {
        HubMetadataPublicKeyStore hubMetadataPublicKeyStore = new HubMetadataPublicKeyStore(metadataResolver, new PublicKeyFactory(), TestEntityIds.HUB_ENTITY_ID);
        List<PublicKey> verifyingKeysForEntity = hubMetadataPublicKeyStore.getVerifyingKeysForEntity();
        assertThat(verifyingKeysForEntity).containsOnly(getX509Key(TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT), getX509Key(TestCertificateStrings.HUB_TEST_SECONDARY_PUBLIC_SIGNING_CERT));
    }

    @Test
    public void shouldErrorWhenMetadataIsInvalid() throws Exception {
        HubMetadataPublicKeyStore hubMetadataPublicKeyStore = new HubMetadataPublicKeyStore(invalidMetadataResolver, new PublicKeyFactory(), TestEntityIds.HUB_ENTITY_ID);
        try {
            hubMetadataPublicKeyStore.getVerifyingKeysForEntity();
            fail("we expected the HubEntityMissingException");
        } catch(HubEntityMissingException e) {
            assertThat(e).hasMessage("The HUB entity-id: \"https://signin.service.gov.uk\" could not be found in the metadata. Metadata could be expired, invalid, or missing entities");
        }
    }

    @Test
    public void shouldErrorWhenMetadataIsEmpty() throws Exception {
        HubMetadataPublicKeyStore hubMetadataPublicKeyStore = new HubMetadataPublicKeyStore(emptyMetadataResolver, new PublicKeyFactory(), TestEntityIds.HUB_ENTITY_ID);
        try {
            hubMetadataPublicKeyStore.getVerifyingKeysForEntity();
            fail("we expected the HubEntityMissingException");
        } catch(HubEntityMissingException e) {
            assertThat(e).hasMessage("The HUB entity-id: \"https://signin.service.gov.uk\" could not be found in the metadata. Metadata could be expired, invalid, or missing entities");
        }
    }

}
