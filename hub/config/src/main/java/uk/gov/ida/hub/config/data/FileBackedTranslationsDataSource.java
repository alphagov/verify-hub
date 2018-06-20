package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.TranslationData;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedTranslationsDataSource extends FileBackedConfigDataSource<TranslationData> {
    private static final String TRANSACTIONS_DATA_DIRECTORY = "../../display-locales/transactions";

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
                TRANSACTIONS_DATA_DIRECTORY
        );
    }
}
