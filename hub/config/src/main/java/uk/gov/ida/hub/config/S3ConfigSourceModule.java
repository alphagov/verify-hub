package uk.gov.ida.hub.config;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.data.S3ConfigSource;

import javax.inject.Singleton;

public class S3ConfigSourceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    @SuppressWarnings("unused")
    private S3ConfigSource getS3ConfigSource(ConfigConfiguration configConfiguration, ObjectMapper objectMapper) {
        SelfServiceConfig selfServiceConfig = configConfiguration.getSelfService();
        if (selfServiceConfig.isEnabled()) {
            return new S3ConfigSource(
                    selfServiceConfig,
                    AmazonS3ClientBuilder.standard().withRegion("eu-west-2").build(),
                    objectMapper);
        }
        return new S3ConfigSource();
    }
}

