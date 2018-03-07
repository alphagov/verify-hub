package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.Organization;
import uk.gov.ida.common.shared.security.IdGenerator;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.domain.SamlEndpointDto;

import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;

public class HubIdentityProviderMetadataDtoToEntityDescriptorTransformer implements Function<HubIdentityProviderMetadataDto,EntityDescriptor> {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;
    private final KeyDescriptorsUnmarshaller keyDescriptorsUnmarshaller;
    private final IdGenerator idGenerator;

    public HubIdentityProviderMetadataDtoToEntityDescriptorTransformer(
        OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
        KeyDescriptorsUnmarshaller keyDescriptorsUnmarshaller,
        IdGenerator idGenerator) {

        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
        this.keyDescriptorsUnmarshaller = keyDescriptorsUnmarshaller;
        this.idGenerator = idGenerator;
    }

    @Override
    public EntityDescriptor apply(HubIdentityProviderMetadataDto dto) {
        final EntityDescriptor entityDescriptor = doTransform(dto);

        final List<KeyDescriptor> keyDescriptors = entityDescriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS).getKeyDescriptors();
        keyDescriptors.addAll(getKeyDescriptorsUnmarshaller().fromCertificates(dto.getIdpSigningCertificates()));
        keyDescriptors.addAll(getKeyDescriptorsUnmarshaller().fromCertificates(dto.getEncryptionCertificates()));


        return entityDescriptor;
    }

    public EntityDescriptor doTransform(HubIdentityProviderMetadataDto dto) {
        EntityDescriptor entityDescriptor = openSamlXmlObjectFactory.createEntityDescriptor();

        entityDescriptor.setID(idGenerator.getId());
        entityDescriptor.setEntityID(dto.getEntityId());
        entityDescriptor.setValidUntil(dto.getValidUntil());

        IDPSSODescriptor idpSsoDescriptor = openSamlXmlObjectFactory.createIDPSSODescriptor();
        idpSsoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);

        transformSingleSignOnServiceEndpoints(idpSsoDescriptor, dto);

        List <KeyDescriptor> signingKeyDescriptors = keyDescriptorsUnmarshaller.fromCertificates(newArrayList(dto.getSigningCertificates()));
        idpSsoDescriptor.getKeyDescriptors().addAll(signingKeyDescriptors);

        entityDescriptor.getRoleDescriptors().add(idpSsoDescriptor);

        return entityDescriptor;
    }

    private void transformSingleSignOnServiceEndpoints(IDPSSODescriptor idpSsoDescriptor, HubIdentityProviderMetadataDto dto) {
        for (SamlEndpointDto endpoint : dto.getSingleSignOnEndpoints()) {
            String bindingUri = SAMLConstants.SAML2_POST_BINDING_URI; // These will always be post
            idpSsoDescriptor.getSingleSignOnServices().add(openSamlXmlObjectFactory.createSingleSignOnService(bindingUri, endpoint.getLocation().toASCIIString()));
        }
    }

    protected KeyDescriptorsUnmarshaller getKeyDescriptorsUnmarshaller() {
        return keyDescriptorsUnmarshaller;
    }

}
