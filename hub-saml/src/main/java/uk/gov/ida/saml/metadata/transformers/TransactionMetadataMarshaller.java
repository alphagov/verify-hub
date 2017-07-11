package uk.gov.ida.saml.metadata.transformers;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.credential.UsageType;
import uk.gov.ida.common.shared.security.Certificate;
import uk.gov.ida.saml.metadata.domain.AssertionConsumerServiceEndpointDto;
import uk.gov.ida.saml.metadata.domain.ContactPersonDto;
import uk.gov.ida.saml.metadata.domain.HubServiceProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.OrganisationDto;

import java.util.List;

public class TransactionMetadataMarshaller {
    private OrganizationMarshaller organizationMarshaller;
    private ContactPersonsMarshaller contactPersonsMarshaller;
    private KeyDescriptorFinder keyDescriptorFinder;
    private KeyDescriptorMarshaller keyDescriptorMarshaller;
    private AssertionConsumerServicesMarshaller assertionConsumerServiceMarshaller;
    private ValidUntilExtractor validUntilExtractor;

    public TransactionMetadataMarshaller(
            OrganizationMarshaller organizationMarshaller,
            ContactPersonsMarshaller contactPersonsMarshaller,
            KeyDescriptorMarshaller keyDescriptorMarshaller,
            KeyDescriptorFinder keyDescriptorFinder,
            AssertionConsumerServicesMarshaller assertionConsumerServiceMarshaller,
            ValidUntilExtractor validUntilExtractor) {
        this.organizationMarshaller = organizationMarshaller;
        this.contactPersonsMarshaller = contactPersonsMarshaller;
        this.keyDescriptorMarshaller = keyDescriptorMarshaller;
        this.keyDescriptorFinder = keyDescriptorFinder;
        this.assertionConsumerServiceMarshaller = assertionConsumerServiceMarshaller;
        this.validUntilExtractor = validUntilExtractor;
    }

    public HubServiceProviderMetadataDto toDto(EntityDescriptor entityDescriptor) {
        SPSSODescriptor spServiceDescriptor = entityDescriptor.getSPSSODescriptor(SAMLConstants.SAML20P_NS);

        String entityId = entityDescriptor.getEntityID();

        List<AssertionConsumerServiceEndpointDto> samlEndpointDtos = assertionConsumerServiceMarshaller.toDto(spServiceDescriptor.getAssertionConsumerServices());
        OrganisationDto organisationDto = organizationMarshaller.toDto(entityDescriptor.getOrganization());

        List<ContactPersonDto> transformedContactPersons = (contactPersonsMarshaller.toDto(entityDescriptor.getContactPersons()));

        final List<KeyDescriptor> keyDescriptors = spServiceDescriptor.getKeyDescriptors();
        KeyDescriptor signingKeyDescriptor =
                keyDescriptorFinder.find(keyDescriptors, UsageType.SIGNING, entityId);
        final Certificate signingCertificate =
                keyDescriptorMarshaller.toCertificate(signingKeyDescriptor);
        KeyDescriptor encryptionKeyDescriptor =
                keyDescriptorFinder.find(keyDescriptors, UsageType.ENCRYPTION, entityId);
        final Certificate encryptionCertificate =
                keyDescriptorMarshaller.toCertificate(encryptionKeyDescriptor);
        DateTime validUntil = validUntilExtractor.extract(entityDescriptor);
        return new HubServiceProviderMetadataDto(
                entityId,
                validUntil,
                organisationDto,
                transformedContactPersons,
                signingCertificate,
                ImmutableList.of(encryptionCertificate),
                samlEndpointDtos);
    }
}
