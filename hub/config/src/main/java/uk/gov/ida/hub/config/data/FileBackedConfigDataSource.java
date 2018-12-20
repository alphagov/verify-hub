package uk.gov.ida.hub.config.data;

import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.exceptions.ConfigValidationException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class FileBackedConfigDataSource<T> implements ConfigDataSource<T> {

    private final ConfigConfiguration configuration;
    private final ConfigurationFactory<T> configurationFactory;
    private final String dataDirectory;

    public FileBackedConfigDataSource(
            ConfigConfiguration configuration,
            ConfigurationFactory<T> configurationFactory,
            String dataDirectory) {

        this.configuration = configuration;
        this.configurationFactory = configurationFactory;
        this.dataDirectory = dataDirectory;
    }

    @Override
    public Collection<T> loadConfig() {
        Collection<T> configData = new ArrayList<>();
        final File configDirectory = new File(configuration.getDataDirectory(), dataDirectory);
        final File[] dataFiles = configDirectory.listFiles((FilenameFilter) new WildcardFileFilter("*.yml"));
        if (dataFiles == null) {
            throw ConfigValidationException.createFileReadError(configDirectory.getAbsolutePath());
        }

        for (File dataFile : dataFiles) {
            T data;
            try {
                data = configurationFactory.build(dataFile);
            } catch (IOException | ConfigurationException e) {
                throw new RuntimeException(e);
            }
            configData.add(data);

        }
        return configData;
    }
}
