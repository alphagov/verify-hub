package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.saml.metadata.domain.HubIdentityProviderMetadataDto;
import uk.gov.ida.saml.metadata.transformers.decorators.SamlEntityDescriptorValidator;

import java.util.function.Function;


public class EntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer implements Function<EntityDescriptor, HubIdentityProviderMetadataDto> {
    private final IdentityProviderMetadataMarshaller marshaller;
    private final SamlEntityDescriptorValidator entityDescriptorValidator;

    public EntityDescriptorToHubIdentityProviderMetadataDtoValidatingTransformer(
            final IdentityProviderMetadataMarshaller marshaller,
            final SamlEntityDescriptorValidator entityDescriptorValidator) {

        this.marshaller = marshaller;
        this.entityDescriptorValidator = entityDescriptorValidator;
    }

    @Override
    public HubIdentityProviderMetadataDto apply(final EntityDescriptor entityDescriptor) {
        entityDescriptorValidator.validate(entityDescriptor);
        return marshaller.toDto(entityDescriptor);
    }

}
