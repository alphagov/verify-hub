package uk.gov.ida.saml.metadata.transformers;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder;
import uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.net.URI;
import java.util.Iterator;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.CertificateBuilder.aCertificate;
import static uk.gov.ida.saml.core.test.builders.ContactPersonDtoBuilder.aContactPersonDto;
import static uk.gov.ida.saml.core.test.builders.OrganisationDtoBuilder.anOrganisationDto;
import static uk.gov.ida.saml.core.test.builders.metadata.CompanyBuilder.aCompany;
import static uk.gov.ida.saml.core.test.builders.metadata.ContactPersonBuilder.aContactPerson;
import static uk.gov.ida.saml.core.test.builders.metadata.EntityDescriptorBuilder.anEntityDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.IdpSsoDescriptorBuilder.anIdpSsoDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyDescriptorBuilder.aKeyDescriptor;
import static uk.gov.ida.saml.core.test.builders.metadata.KeyInfoBuilder.aKeyInfo;
import static uk.gov.ida.saml.core.test.builders.metadata.OrganizationBuilder.anOrganization;
import static uk.gov.ida.saml.core.test.builders.metadata.X509CertificateBuilder.aX509Certificate;
import static uk.gov.ida.saml.core.test.builders.metadata.X509DataBuilder.aX509Data;

@RunWith(OpenSAMLMockitoRunner.class)
public class IdentityProviderMetadataMarshallerTest {

    @Mock
    private OrganizationMarshaller organizationMarshaller;
    @Mock
    private ContactPersonsMarshaller contactPersonsUnmarshaller;
    @Mock
    private SingleSignOnServicesMarshaller singleSignOnServicesMarshaller;
    @Mock
    private KeyDescriptorMarshaller keyDescriptorMarshaller;
    @Mock
    private ValidUntilExtractor validUntilExtractor;

    private IdentityProviderMetadataMarshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = new IdentityProviderMetadataMarshaller(organizationMarshaller, contactPersonsUnmarshaller, singleSignOnServicesMarshaller, keyDescriptorMarshaller, validUntilExtractor, TestEntityIds.HUB_ENTITY_ID);
    }

    @Test
    public void transform_shouldTransformSingleSignOnService() throws Exception {
        IDPSSODescriptor idpssoDescriptor = anIdpSsoDescriptor().build();
        final SamlEndpointDto postBinding = SamlEndpointDto.createPostBinding(URI.create("/"));
        when(singleSignOnServicesMarshaller.toDto(idpssoDescriptor.getSingleSignOnServices())).thenReturn(asList(postBinding));
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());

        final HubIdentityProviderMetadataDto metadataDto =
                marshaller.toDto(anEntityDescriptor().withIdpSsoDescriptor(idpssoDescriptor).build());

        assertThat(metadataDto.getSingleSignOnEndpoints()).contains(postBinding);
    }

    @Test
    public void transform_shouldTransformEntityId() throws Exception {
        String entityId = UUID.randomUUID().toString();
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());

        final HubIdentityProviderMetadataDto result =
                marshaller.toDto(anEntityDescriptor().withEntityId(entityId).build());

        assertThat(result.getEntityId()).isEqualTo(entityId);
    }

    @Test
    public void transform_shouldTransformSigningCertificate() throws Exception {
        Certificate transformedCertificate = aCertificate().withIssuerId(TestEntityIds.HUB_ENTITY_ID).build();
        KeyDescriptor keyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Signing.toString())
                .withKeyInfo(aKeyInfo()
                        .withX509Data(aX509Data()
                                .withX509Certificate(aX509Certificate().build())
                                .build())
                        .build())
                .build();
        IDPSSODescriptor idpssoDescriptor = anIdpSsoDescriptor().addKeyDescriptor(keyDescriptor).build();
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());
        when(keyDescriptorMarshaller.toCertificate(keyDescriptor)).thenReturn(transformedCertificate);

        final HubIdentityProviderMetadataDto result = marshaller.toDto(anEntityDescriptor().withIdpSsoDescriptor(idpssoDescriptor).build());

        assertThat(result.getSigningCertificates()).hasSize(1);
        assertThat(result.getSigningCertificates().get(0)).isEqualTo(transformedCertificate);
    }

    @Test
    public void transform_shouldTransformMultipleHubSigningCertificates() throws Exception {

        Certificate transformedPrimaryCertificate = aCertificate().withIssuerId(TestEntityIds.HUB_ENTITY_ID).build();
        Certificate transformedSecondaryCertificate = aCertificate().withIssuerId(TestEntityIds.HUB_SECONDARY_ENTITY_ID).build();

        KeyDescriptor primaryKeyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Signing.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo()
                        .withX509Data(X509DataBuilder.aX509Data()
                                .withX509Certificate(X509CertificateBuilder.aX509Certificate().build())
                                .build())
                        .build())
                .build();

        KeyDescriptor secondaryKeyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Signing.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo()
                        .withX509Data(X509DataBuilder.aX509Data()
                                .withX509Certificate(X509CertificateBuilder.aX509Certificate().build())
                                .build())
                        .build())
                .build();

        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());
        when(keyDescriptorMarshaller.toCertificate(primaryKeyDescriptor)).thenReturn(transformedPrimaryCertificate);
        when(keyDescriptorMarshaller.toCertificate(secondaryKeyDescriptor)).thenReturn(transformedSecondaryCertificate);

        IDPSSODescriptor idpssoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor().addKeyDescriptor(primaryKeyDescriptor).addKeyDescriptor(secondaryKeyDescriptor).build();

        final HubIdentityProviderMetadataDto result = marshaller.toDto(anEntityDescriptor().withIdpSsoDescriptor(idpssoDescriptor).build());

        assertThat(result.getSigningCertificates().size()).isEqualTo(2);
        assertThat(result.getSigningCertificates()).contains(transformedPrimaryCertificate);
        assertThat(result.getSigningCertificates()).contains(transformedSecondaryCertificate);
    }

    @Test
    public void transform_shouldTransformIdpSigningCertificates() throws Exception {
        String idpOneEntityId = UUID.randomUUID().toString();
        String idpTwoEntityId = UUID.randomUUID().toString();
        Certificate idpCertOne = aCertificate().withIssuerId(idpOneEntityId).build();
        Certificate idpCertTwo = aCertificate().withIssuerId(idpTwoEntityId).build();
        final KeyDescriptor idpOneKeyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Signing.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo().withKeyName(idpOneEntityId).build())
                .build();
        final KeyDescriptor idpTwoKeyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Signing.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo().withKeyName(idpTwoEntityId).build())
                .build();
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());
        when(keyDescriptorMarshaller.toCertificate(idpOneKeyDescriptor)).thenReturn(idpCertOne);
        when(keyDescriptorMarshaller.toCertificate(idpTwoKeyDescriptor)).thenReturn(idpCertTwo);
        final IDPSSODescriptor idpSsoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor()
                .addKeyDescriptor(idpOneKeyDescriptor)
                .addKeyDescriptor(idpTwoKeyDescriptor)
                .build();
        final EntityDescriptor entityDescriptor = anEntityDescriptor().withIdpSsoDescriptor(idpSsoDescriptor).build();

        final HubIdentityProviderMetadataDto result = marshaller.toDto(entityDescriptor);

        assertThat(result.getIdpSigningCertificates()).contains(idpCertOne);
        assertThat(result.getIdpSigningCertificates()).contains(idpCertTwo);
    }

    @Test
    public void transform_shouldTransformEncryptionCertificate() throws Exception {
        Certificate transformedCertificate = aCertificate()
                .withIssuerId(TestEntityIds.HUB_ENTITY_ID)
                .withKeyUse(Certificate.KeyUse.Encryption)
                .build();
        KeyDescriptor keyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Encryption.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo()
                        .withX509Data(X509DataBuilder.aX509Data()
                                .withX509Certificate(X509CertificateBuilder.aX509Certificate().build())
                                .build())
                        .build())
                .build();
        IDPSSODescriptor idpssoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor().addKeyDescriptor(keyDescriptor).build();
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());
        when(keyDescriptorMarshaller.toCertificate(keyDescriptor)).thenReturn(transformedCertificate);

        final HubIdentityProviderMetadataDto result = marshaller.toDto(anEntityDescriptor().withIdpSsoDescriptor(idpssoDescriptor).build());

        assertThat(result.getEncryptionCertificates()).hasSize(1);
        assertThat(result.getEncryptionCertificates().get(0)).isEqualTo(transformedCertificate);
    }

    @Test
    public void transform_shouldTransformEncryptionAndSigningCertificates() throws Exception {
        Certificate transformedSigningCertificate = aCertificate()
                .withIssuerId(TestEntityIds.HUB_ENTITY_ID)
                .withKeyUse(Certificate.KeyUse.Signing)
                .build();
        Certificate transformedEncryptionCertificate = aCertificate()
                .withIssuerId(TestEntityIds.HUB_ENTITY_ID)
                .withKeyUse(Certificate.KeyUse.Encryption)
                .build();
        KeyDescriptor signingKeyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Signing.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo()
                        .withX509Data(X509DataBuilder.aX509Data()
                                .withX509Certificate(X509CertificateBuilder.aX509Certificate().build())
                                .build())
                        .build())
                .build();
        KeyDescriptor encryptionKeyDescriptor = aKeyDescriptor()
                .withUse(Certificate.KeyUse.Encryption.toString())
                .withKeyInfo(KeyInfoBuilder.aKeyInfo()
                        .withX509Data(X509DataBuilder.aX509Data()
                                .withX509Certificate(X509CertificateBuilder.aX509Certificate().build())
                                .build())
                        .build())
                .build();
        IDPSSODescriptor idpssoDescriptor = IdpSsoDescriptorBuilder.anIdpSsoDescriptor()
                .addKeyDescriptor(signingKeyDescriptor)
                .addKeyDescriptor(encryptionKeyDescriptor)
                .build();
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());
        when(keyDescriptorMarshaller.toCertificate(signingKeyDescriptor)).thenReturn(transformedSigningCertificate);
        when(keyDescriptorMarshaller.toCertificate(encryptionKeyDescriptor)).thenReturn(transformedEncryptionCertificate);

        final HubIdentityProviderMetadataDto result = marshaller.toDto(anEntityDescriptor().withIdpSsoDescriptor(idpssoDescriptor).build());

        assertThat(result.getSigningCertificates()).hasSize(1);
        assertThat(result.getSigningCertificates().get(0)).isEqualTo(transformedSigningCertificate);
        assertThat(result.getEncryptionCertificates()).hasSize(1);
        assertThat(result.getEncryptionCertificates().get(0)).isEqualTo(transformedEncryptionCertificate);
    }

    @Test
    public void transform_shouldTransformOrganization() throws Exception {
        final OrganisationDto transformedOrganisation = anOrganisationDto().build();
        final Organization organization = anOrganization().build();
        when(organizationMarshaller.toDto(organization)).thenReturn(transformedOrganisation);
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());

        final HubIdentityProviderMetadataDto result = marshaller.toDto(anEntityDescriptor().withOrganization(organization).build());

        assertThat(result.getOrganisation()).isEqualTo(transformedOrganisation);
    }

    @Test
    public void transform_shouldTransformContactPersons() throws Exception {
        final ContactPerson contactPersonOne = aContactPerson().withCompany(aCompany().withName("Foo").build()).build();
        final ContactPerson contactPersonTwo = aContactPerson().withCompany(aCompany().withName("Bar").build()).build();
        final ContactPersonDto transformedContactPersonOne = aContactPersonDto().withCompanyName("Foo").build();
        final ContactPersonDto transformedContactPersonTwo = aContactPersonDto().withCompanyName("Bar").build();
        when(contactPersonsUnmarshaller.toDto(asList(contactPersonOne, contactPersonTwo))).thenReturn(asList(transformedContactPersonOne, transformedContactPersonTwo));
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());

        final HubIdentityProviderMetadataDto result = marshaller.toDto(anEntityDescriptor().addContactPerson(contactPersonOne).addContactPerson(contactPersonTwo).build());

        assertThat(result.getContactPersons().size()).isEqualTo(2);
        final Iterator<ContactPersonDto> contactPersonsIterator = result.getContactPersons().iterator();
        assertThat(contactPersonsIterator.next()).isEqualTo(transformedContactPersonOne);
        assertThat(contactPersonsIterator.next()).isEqualTo(transformedContactPersonTwo);
    }

    @Test
    public void transform_shouldTransformExpires() throws Exception {
        final EntityDescriptor entityDescriptor = anEntityDescriptor().build();
        DateTime expires = new DateTime();
        when(validUntilExtractor.extract(entityDescriptor)).thenReturn(expires);
        when(keyDescriptorMarshaller.toCertificate(any(KeyDescriptor.class))).thenReturn(aCertificate().build());

        final HubIdentityProviderMetadataDto metadataDto = marshaller.toDto(entityDescriptor);

        org.assertj.jodatime.api.Assertions.assertThat(metadataDto.getValidUntil()).isEqualTo(expires);
    }
}
