package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.CountryConfig;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedCountryConfigDataSource extends FileBackedConfigDataSource<CountryConfig> {

    public static final String COUNTRIES_DATA_DIRECTORY = "countries";

    @Inject
    public FileBackedCountryConfigDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactoryFactory<CountryConfig> configurationFactoryFactory,
            ObjectMapper objectMapper) {
        super(
                configuration,
                configurationFactoryFactory.create(CountryConfig.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"),
                COUNTRIES_DATA_DIRECTORY
        );
    }
}
