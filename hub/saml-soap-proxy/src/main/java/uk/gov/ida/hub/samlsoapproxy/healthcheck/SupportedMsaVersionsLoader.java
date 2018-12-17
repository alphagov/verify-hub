package uk.gov.ida.hub.samlsoapproxy.healthcheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.UrlConfigurationSourceProvider;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;

import static javax.validation.Validation.buildDefaultValidatorFactory;

public class SupportedMsaVersionsLoader {
    private final ConfigurationFactoryFactory<SupportedMsaVersions> supportedMsaVersionsFactoryFactory;
    private final ObjectMapper objectMapper;
    private final UrlConfigurationSourceProvider configurationSourceProvider;

    @Inject
    public SupportedMsaVersionsLoader(
            final ConfigurationFactoryFactory<SupportedMsaVersions> supportedMsaVersionsFactoryFactory,
            final ObjectMapper objectMapper,
            final UrlConfigurationSourceProvider configurationSourceProvider) {

        this.supportedMsaVersionsFactoryFactory = supportedMsaVersionsFactoryFactory;
        this.objectMapper = objectMapper;
        this.configurationSourceProvider = configurationSourceProvider;
    }

    public SupportedMsaVersions loadSupportedMsaVersions(final URL url) {
        final ConfigurationFactory<SupportedMsaVersions> factory = supportedMsaVersionsFactoryFactory.create(
                SupportedMsaVersions.class,
                buildDefaultValidatorFactory().getValidator(),
                objectMapper,
                "");
        try {
            SupportedMsaVersions supportedMsaVersions = factory.build(
                    configurationSourceProvider,
                    url.toString());
            return supportedMsaVersions;
        } catch (IOException | ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
