package uk.gov.ida.saml.metadata.transformers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.credential.UsageType;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder;
import uk.gov.ida.saml.metadata.domain.AssertionConsumerServiceEndpointDto;
import uk.gov.ida.saml.metadata.domain.HubServiceProviderMetadataDto;

import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.CertificateBuilder.aCertificate;
import static uk.gov.ida.saml.core.test.builders.metadata.AssertionConsumerServiceBuilder.anAssertionConsumerService;
import static uk.gov.ida.saml.core.test.builders.metadata.AssertionConsumerServiceEndpointDtoBuilder.anAssertionConsumerServiceEndpointDto;
import static uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder.anEntityDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder.aKeyDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder.aKeyInfo;
import static uk.gov.ida.saml.core.test.builders.metadata.SPSSODescriptorBuilder.anSpServiceDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder.aX509Data;

@RunWith(OpenSAMLMockitoRunner.class)
public class TransactionMetadataMarshallerTest {
    @Mock
    private OrganizationMarshaller organizationMarshaller;
    @Mock
    private ContactPersonsMarshaller contactPersonsMarshaller;
    @Mock
    private KeyDescriptorMarshaller keyDescriptorMarshaller;
    @Mock
    private KeyDescriptorFinder keyDescriptorFinder;
    @Mock
    private ValidUntilExtractor validUntilExtractor;
    @Mock
    private AssertionConsumerServicesMarshaller assertionConsumerServiceMarshaller;

    private TransactionMetadataMarshaller marshaller;
    private String encryptionCertificateValue = UUID.randomUUID().toString();
    private KeyDescriptor encryptionKeyDescriptor;

    @Before
    public void setUp() throws Exception {
        encryptionKeyDescriptor = aKeyDescriptor().withKeyInfo(aKeyInfo().withKeyName(null).build()).withUse(UsageType.ENCRYPTION.toString()).build();
        when(keyDescriptorMarshaller.toCertificate(encryptionKeyDescriptor)).thenReturn(aCertificate().withCertificate(encryptionCertificateValue).build());

        marshaller = new TransactionMetadataMarshaller(organizationMarshaller, contactPersonsMarshaller, keyDescriptorMarshaller, keyDescriptorFinder, assertionConsumerServiceMarshaller, validUntilExtractor);
    }

    @Test
    public void transform_shouldTransformSigningCertificate() throws Exception {
        String signingCertificateValue = UUID.randomUUID().toString();
        final KeyDescriptor signingKeyDescriptor = aKeyDescriptor().withKeyInfo(aKeyInfo().withKeyName(null).withX509Data(aX509Data().withX509Certificate(X509CertificateBuilder.aX509Certificate().withCert(signingCertificateValue).build()).build()).build()).withUse(UsageType.SIGNING.toString()).build();
        final SPSSODescriptor spServiceDescriptor = anSpServiceDescriptor().withoutDefaultSigningKey().addKeyDescriptor(signingKeyDescriptor).build();
        when(keyDescriptorMarshaller.toCertificate(signingKeyDescriptor)).thenReturn(aCertificate().withCertificate(signingCertificateValue).build());
        when(keyDescriptorFinder.find(eq(spServiceDescriptor.getKeyDescriptors()), eq(UsageType.ENCRYPTION), anyString())).thenReturn(encryptionKeyDescriptor);
        when(keyDescriptorFinder.find(eq(spServiceDescriptor.getKeyDescriptors()), eq(UsageType.SIGNING), anyString())).thenReturn(signingKeyDescriptor);

        final HubServiceProviderMetadataDto result =
                marshaller.toDto(anEntityDescriptor().addSpServiceDescriptor(spServiceDescriptor).build());

        assertThat(result.getSigningCertificates()).hasSize(1);
        assertThat(result.getSigningCertificates().get(0).getCertificate()).isEqualTo(signingCertificateValue);
    }

    @Test
    public void transform_shouldTransformSigningCertificateForCorrectEntity() throws Exception {
        String signingCertificateValue = UUID.randomUUID().toString();
        final KeyDescriptor rightKeyDescriptor = aKeyDescriptor().withKeyInfo(aKeyInfo().build()).withUse(UsageType.SIGNING.toString()).build();
        final String entityId = rightKeyDescriptor.getKeyInfo().getKeyNames().get(0).getValue();
        final SPSSODescriptor spServiceDescriptor = anSpServiceDescriptor().withoutDefaultSigningKey().addKeyDescriptor(rightKeyDescriptor).addKeyDescriptor(aKeyDescriptor().withUse(UsageType.ENCRYPTION.toString()).withKeyInfo(aKeyInfo().withKeyName(entityId).build()).build()).build();
        when(keyDescriptorMarshaller.toCertificate(rightKeyDescriptor)).thenReturn(aCertificate().withCertificate(signingCertificateValue).build());
        when(keyDescriptorFinder.find(eq(spServiceDescriptor.getKeyDescriptors()), eq(UsageType.ENCRYPTION), anyString())).thenReturn(encryptionKeyDescriptor);
        when(keyDescriptorFinder.find(eq(spServiceDescriptor.getKeyDescriptors()), eq(UsageType.SIGNING), anyString())).thenReturn(rightKeyDescriptor);

        final HubServiceProviderMetadataDto result =
                marshaller.toDto(anEntityDescriptor().withEntityId(entityId).addSpServiceDescriptor(spServiceDescriptor).build());

        assertThat(result.getSigningCertificates()).hasSize(1);
        assertThat(result.getSigningCertificates().get(0).getCertificate()).isEqualTo(signingCertificateValue);
    }

    @Test
    public void transform_shouldTransformEncryptionCertificate() throws Exception {
        final SPSSODescriptor spServiceDescriptor = anSpServiceDescriptor().withoutDefaultSigningKey().addKeyDescriptor(aKeyDescriptor().withUse(UsageType.ENCRYPTION.toString()).build()).build();
        when(keyDescriptorFinder.find(eq(spServiceDescriptor.getKeyDescriptors()), eq(UsageType.ENCRYPTION), anyString())).thenReturn(encryptionKeyDescriptor);
        final HubServiceProviderMetadataDto result =
                marshaller.toDto(
                        anEntityDescriptor().addSpServiceDescriptor(spServiceDescriptor).build());

        assertThat(result.getEncryptionCertificates().size()).isEqualTo(1);
        assertThat(result.getEncryptionCertificates().get(0).getCertificate()).isEqualTo(encryptionCertificateValue);
    }

    @Test
    public void transform_shouldTransformAssertionConsumerServiceBindings() throws Exception {
        String locationOne = "/foo";
        AssertionConsumerService assertionConsumerService1 = anAssertionConsumerService()
                .withBinding(SAMLConstants.SAML2_POST_BINDING_URI)
                .withLocation(locationOne)
                .build();

        String locationTwo = "/bar";
        AssertionConsumerService assertionConsumerService2 = anAssertionConsumerService()
                .withBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI)
                .withLocation(locationTwo)
                .build();

        final SPSSODescriptor spServiceDescriptor = anSpServiceDescriptor()
                .addAssertionConsumerService(assertionConsumerService1)
                .addAssertionConsumerService(assertionConsumerService2)
                .build();

        final KeyDescriptor keyDescriptor = aKeyDescriptor().build();

        when(keyDescriptorMarshaller.toCertificate(keyDescriptor)).thenReturn(aCertificate().build());
        when(keyDescriptorFinder.find(eq(spServiceDescriptor.getKeyDescriptors()), eq(UsageType.ENCRYPTION), anyString())).thenReturn(keyDescriptor);

        List<AssertionConsumerServiceEndpointDto> expectedList = asList(anAssertionConsumerServiceEndpointDto().build());
        when(assertionConsumerServiceMarshaller.toDto(asList(assertionConsumerService1, assertionConsumerService2))).thenReturn(expectedList);

        final HubServiceProviderMetadataDto result =
                marshaller.toDto(anEntityDescriptor().addSpServiceDescriptor(spServiceDescriptor).build());

        assertThat(result.getAssertionConsumerServiceBindings()).isEqualTo(expectedList);
    }

}
