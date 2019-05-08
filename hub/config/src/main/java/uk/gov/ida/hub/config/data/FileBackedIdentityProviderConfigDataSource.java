package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedIdentityProviderConfigDataSource extends FileBackedConfigDataSource<IdentityProviderConfig> {

    public static final String IDENTITY_PROVIDER_DATA_DIRECTORY = "idps";

    @Inject
    public FileBackedIdentityProviderConfigDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactoryFactory<IdentityProviderConfig> configurationFactoryFactory,
            ObjectMapper objectMapper) {
        super(
                configuration,
                configurationFactoryFactory.create(IdentityProviderConfig.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"),
                IDENTITY_PROVIDER_DATA_DIRECTORY
        );
    }
}
