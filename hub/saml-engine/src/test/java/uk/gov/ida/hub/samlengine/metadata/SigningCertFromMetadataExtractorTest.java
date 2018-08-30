package uk.gov.ida.hub.samlengine.metadata;

import com.google.common.collect.ImmutableList;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlengine.exceptions.CertificateForCurrentPrivateSigningKeyNotFoundInMetadataException;
import uk.gov.ida.hub.samlengine.exceptions.UnableToResolveSigningCertsForHubException;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.SPSSODescriptorBuilder;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;

@RunWith(MockitoJUnitRunner.class)
public class SigningCertFromMetadataExtractorTest {

    private SigningCertFromMetadataExtractor signingCertFromMetadataExtractor;

    private static EntityDescriptor hubEntityDescriptor;

    @BeforeClass
    public static void beforeClass() throws MarshallingException, SignatureException {

        hubEntityDescriptor = EntityDescriptorBuilder.anEntityDescriptor()
                .withEntityId(HUB_ENTITY_ID)
                .addSpServiceDescriptor(SPSSODescriptorBuilder.anSpServiceDescriptor()
                        .build())
                .build();
    }

    private X509Certificate hubSigningCert = new X509CertificateFactory().createCertificate(TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(HUB_ENTITY_ID));

    private PublicKey notHubSigningPublicKey = new X509CertificateFactory().createCertificate(TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(TEST_RP)).getPublicKey();

    @Mock
    private MetadataResolver metadataResolver;

    @Test
    public void certIsSuccessfullyExtractedFromMetadata() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenReturn(ImmutableList.of(hubEntityDescriptor));
        final X509Certificate x509Certificate = signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(hubSigningCert.getPublicKey());
        assertThat(x509Certificate).isEqualTo(hubSigningCert);
    }

    @Test(expected = CertificateForCurrentPrivateSigningKeyNotFoundInMetadataException.class)
    public void certIsNotFoundInMetadata() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenReturn(ImmutableList.of());
        signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(notHubSigningPublicKey);
    }

    @Test(expected = UnableToResolveSigningCertsForHubException.class)
    public void unableToResolveSigningCert() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenThrow(new ResolverException());
        signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(hubSigningCert.getPublicKey());
    }

}
