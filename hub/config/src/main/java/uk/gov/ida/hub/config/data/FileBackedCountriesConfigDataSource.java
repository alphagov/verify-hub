package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedCountriesConfigDataSource extends FileBackedConfigDataSource<CountriesConfigEntityData> {

    public static final String COUNTRIES_DATA_DIRECTORY = "countries";

    @Inject
    public FileBackedCountriesConfigDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactoryFactory<CountriesConfigEntityData> configurationFactoryFactory,
            ObjectMapper objectMapper) {
        super(
                configuration,
                configurationFactoryFactory.create(CountriesConfigEntityData.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"),
                COUNTRIES_DATA_DIRECTORY
        );
    }
}
