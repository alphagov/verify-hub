package uk.gov.ida.saml.metadata.transformers;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.saml.metadata.domain.HubServiceProviderMetadataDto;
import uk.gov.ida.saml.metadata.transformers.decorators.SamlEntityDescriptorValidator;

import java.util.function.Function;

public class EntityDescriptorToHubTransactionMetadataDtoValidatingTransformer implements Function<EntityDescriptor, HubServiceProviderMetadataDto> {
    private final TransactionMetadataMarshaller marshaller;
    private final SamlEntityDescriptorValidator entityDescriptorValidator;

    public EntityDescriptorToHubTransactionMetadataDtoValidatingTransformer(
            TransactionMetadataMarshaller marshaller,
            SamlEntityDescriptorValidator entityDescriptorValidator) {

        this.marshaller = marshaller;
        this.entityDescriptorValidator = entityDescriptorValidator;
    }
    @Override
    public HubServiceProviderMetadataDto apply(final EntityDescriptor entityDescriptor) {
        entityDescriptorValidator.validate(entityDescriptor);
        return marshaller.toDto(entityDescriptor);
    }

}
