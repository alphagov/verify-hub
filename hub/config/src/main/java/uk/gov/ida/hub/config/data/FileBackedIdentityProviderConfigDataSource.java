package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedIdentityProviderConfigDataSource extends FileBackedConfigDataSource<IdentityProviderConfigEntityData> {

    public static final String IDENTITY_PROVIDER_DATA_DIRECTORY = "idps";

    @Inject
    public FileBackedIdentityProviderConfigDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactoryFactory<IdentityProviderConfigEntityData> configurationFactoryFactory,
            ObjectMapper objectMapper) {
        super(
                configuration,
                configurationFactoryFactory.create(IdentityProviderConfigEntityData.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"),
                IDENTITY_PROVIDER_DATA_DIRECTORY
        );
    }
}
