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
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.signature.support.SignatureException;
import uk.gov.ida.common.shared.security.X509CertificateFactory;
import uk.gov.ida.hub.samlengine.exceptions.CertificateForCurrentPrivateSigningKeyNotFoundInMetadataException;
import uk.gov.ida.hub.samlengine.exceptions.UnableToResolveSigningCertsForHubException;
import uk.gov.ida.saml.core.test.TestCertificateStrings;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.SPSSODescriptorBuilder;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.TestEntityIds.HUB_ENTITY_ID;
import static uk.gov.ida.saml.core.test.TestEntityIds.TEST_RP;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder.aKeyDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder.aKeyInfo;
import static uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder.aX509Certificate;
import static uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder.aX509Data;

@RunWith(MockitoJUnitRunner.class)
public class SigningCertFromMetadataExtractorTest {

    private SigningCertFromMetadataExtractor signingCertFromMetadataExtractor;

    private static EntityDescriptor hubEntityDescriptor;

    @BeforeClass
    public static void beforeClass() throws MarshallingException, SignatureException {
        KeyDescriptor secondKeyDescriptor = aKeyDescriptor().withKeyInfo(aKeyInfo().withKeyName(TestEntityIds.HUB_ENTITY_ID).withX509Data(aX509Data().withX509Certificate(aX509Certificate().withCert(TestCertificateStrings.HUB_TEST_SECONDARY_PUBLIC_SIGNING_CERT).build()).build()).build()).withUse(UsageType.SIGNING.toString()).build();
        hubEntityDescriptor = EntityDescriptorBuilder.anEntityDescriptor()
                .withEntityId(HUB_ENTITY_ID)
                .addSpServiceDescriptor(SPSSODescriptorBuilder.anSpServiceDescriptor()
                        .addKeyDescriptor(secondKeyDescriptor)
                        .build())
                .build();
    }

    private X509Certificate hubPrimarySigningCert = new X509CertificateFactory().createCertificate(TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT);
    private X509Certificate hubSecondarySigningCert = new X509CertificateFactory().createCertificate(TestCertificateStrings.HUB_TEST_SECONDARY_PUBLIC_SIGNING_CERT);

    //Intentionally deriving these from a different object instance.
    private PublicKey hubPrimarySigningPublicKey = new X509CertificateFactory().createCertificate(TestCertificateStrings.HUB_TEST_PUBLIC_SIGNING_CERT).getPublicKey();
    private PublicKey hubSecondarySigningPublicKey = new X509CertificateFactory().createCertificate(TestCertificateStrings.HUB_TEST_SECONDARY_PUBLIC_SIGNING_CERT).getPublicKey();

    private PublicKey notHubSigningPublicKey = new X509CertificateFactory().createCertificate(TestCertificateStrings.PUBLIC_SIGNING_CERTS.get(TEST_RP)).getPublicKey();

    @Mock
    private MetadataResolver metadataResolver;

    @Test
    public void certIsSuccessfullyExtractedFromMetadata() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenReturn(ImmutableList.of(hubEntityDescriptor));
        assertThat(signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(hubPrimarySigningPublicKey)).isEqualTo(hubPrimarySigningCert);
    }

    @Test
    public void certIsSuccessfullyExtractedFromMetadataReversed() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenReturn(ImmutableList.of(hubEntityDescriptor));
        assertThat(signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(hubSecondarySigningPublicKey)).isEqualTo(hubSecondarySigningCert);
    }

    @Test(expected = CertificateForCurrentPrivateSigningKeyNotFoundInMetadataException.class)
    public void certIsNotFoundWhenResolvedMetadataDoesNotContainRelevantCert() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenReturn(ImmutableList.of(hubEntityDescriptor));
        signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(notHubSigningPublicKey);
    }

    @Test(expected = CertificateForCurrentPrivateSigningKeyNotFoundInMetadataException.class)
    public void certIsNotFoundWhenEmptyMetadataReturned() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenReturn(ImmutableList.of());
        signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(notHubSigningPublicKey);
    }

    @Test(expected = UnableToResolveSigningCertsForHubException.class)
    public void unableToResolveMetadata() throws ComponentInitializationException, ResolverException {
        signingCertFromMetadataExtractor = new SigningCertFromMetadataExtractor(metadataResolver, HUB_ENTITY_ID);
        when(metadataResolver.resolve(any())).thenThrow(new ResolverException());
        signingCertFromMetadataExtractor.getSigningCertForCurrentSigningKey(hubPrimarySigningCert.getPublicKey());
    }

}
