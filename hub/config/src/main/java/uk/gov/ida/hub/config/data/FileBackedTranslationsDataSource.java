package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.TranslationData;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedTranslationsDataSource extends FileBackedConfigDataSource<TranslationData> {
    @Inject
    public FileBackedTranslationsDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactoryFactory<TranslationData> configurationFactoryFactory,
            ObjectMapper objectMapper
    ) {
        super(
                configuration,
                configurationFactoryFactory.create(
                        TranslationData.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"
                ),
                configuration.getTranslationsDirectory()
        );
    }
}
