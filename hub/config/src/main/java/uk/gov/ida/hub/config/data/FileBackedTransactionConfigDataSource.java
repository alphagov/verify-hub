package uk.gov.ida.hub.config.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.domain.TransactionConfigEntityData;

import javax.inject.Inject;
import javax.validation.Validation;

public class FileBackedTransactionConfigDataSource extends FileBackedConfigDataSource<TransactionConfigEntityData> {

    public static final String TRANSACTIONS_DATA_DIRECTORY = "transactions";

    @Inject
    public FileBackedTransactionConfigDataSource(ConfigConfiguration configuration,
                                                 ConfigurationFactoryFactory<TransactionConfigEntityData> configurationFactoryFactory,
                                                 ObjectMapper objectMapper) {
        super(
                configuration,
                configurationFactoryFactory.create(TransactionConfigEntityData.class,
                        Validation.buildDefaultValidatorFactory().getValidator(),
                        objectMapper,
                        "hub"),
                TRANSACTIONS_DATA_DIRECTORY
        );
    }
}
