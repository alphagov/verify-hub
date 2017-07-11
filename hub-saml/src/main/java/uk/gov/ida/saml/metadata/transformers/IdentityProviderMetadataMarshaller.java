package uk.gov.ida.saml.metadata.transformers;

import org.joda.time.DateTime;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IdentityProviderMetadataMarshaller {
    private final OrganizationMarshaller organizationMarshaller;
    private ContactPersonsMarshaller contactPersonsMarshaller;
    private SingleSignOnServicesMarshaller singleSignOnServicesMarshaller;
    private KeyDescriptorMarshaller keyDescriptorMarshaller;
    private ValidUntilExtractor validUntilExtractor;
    private final String hubEntityId;

    public IdentityProviderMetadataMarshaller(
            OrganizationMarshaller organizationMarshaller,
            ContactPersonsMarshaller contactPersonsMarshaller,
            SingleSignOnServicesMarshaller singleSignOnServicesMarshaller,
            KeyDescriptorMarshaller keyDescriptorMarshaller,
            ValidUntilExtractor validUntilExtractor,
            String hubEntityId) {
        this.organizationMarshaller = organizationMarshaller;
        this.contactPersonsMarshaller = contactPersonsMarshaller;
        this.singleSignOnServicesMarshaller = singleSignOnServicesMarshaller;
        this.keyDescriptorMarshaller = keyDescriptorMarshaller;
        this.validUntilExtractor = validUntilExtractor;
        this.hubEntityId = hubEntityId;
    }

    public HubIdentityProviderMetadataDto toDto(EntityDescriptor entityDescriptor) {
        String entityId = entityDescriptor.getEntityID();
        IDPSSODescriptor idpSsoDescriptor = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);

        final List<SamlEndpointDto> singleSignOnEndpoints =
                singleSignOnServicesMarshaller.toDto(idpSsoDescriptor.getSingleSignOnServices());

        DateTime expires = validUntilExtractor.extract(entityDescriptor);

        OrganisationDto organisation = organizationMarshaller.toDto(entityDescriptor.getOrganization());
        List<ContactPersonDto> contactPersons = contactPersonsMarshaller.toDto(entityDescriptor.getContactPersons());

        Collection<Certificate> idpSigningCertificates = new ArrayList<>();
        Certificate hubEncryptionCertificate = null; //there should only be one hub encryption cert
        List<Certificate> hubSigningCertificates = new ArrayList<>();
        for (KeyDescriptor keyDescriptor : idpSsoDescriptor.getKeyDescriptors()) {
            final Certificate transformedCertificate =
                    keyDescriptorMarshaller.toCertificate(keyDescriptor);

            if (transformedCertificate.getIssuerId().contains(hubEntityId)) {
                if (transformedCertificate.getKeyUse() == Certificate.KeyUse.Signing) {
                    hubSigningCertificates.add(transformedCertificate);
                }
                else if (transformedCertificate.getKeyUse() == Certificate.KeyUse.Encryption) {
                    hubEncryptionCertificate = transformedCertificate;
                }
            } else {
                idpSigningCertificates.add(transformedCertificate);
            }
        }

        return new HubIdentityProviderMetadataDto(
                singleSignOnEndpoints,
                entityId,
                organisation,
                contactPersons,
                idpSigningCertificates,
                expires,
                hubSigningCertificates,
                hubEncryptionCertificate);
    }
}
