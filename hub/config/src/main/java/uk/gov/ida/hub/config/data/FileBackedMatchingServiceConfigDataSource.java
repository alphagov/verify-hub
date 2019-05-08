package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.MatchingServiceConfig;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedMatchingServiceConfigDataSource extends FileBackedConfigDataSource<MatchingServiceConfig> {

    public static final String MATCHING_SERVICE_DATA_DIRECTORY = "matching-services";

    @Inject
    public FileBackedMatchingServiceConfigDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactoryFactory<MatchingServiceConfig> configurationFactoryFactory,
            ObjectMapper objectMapper) {
        super(
                configuration,
                configurationFactoryFactory.create(MatchingServiceConfig.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"),
                MATCHING_SERVICE_DATA_DIRECTORY
        );
    }
}
