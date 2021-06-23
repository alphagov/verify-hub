package uk.gov.ida.integrationtest.hub.config.apprule.support;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import io.dropwizard.setup.Bootstrap;
import uk.gov.ida.hub.config.ConfigApplication;
import uk.gov.ida.hub.config.ConfigConfiguration;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.data.S3ConfigSource;

import javax.inject.Singleton;
import java.util.function.Supplier;

public class ConfigIntegrationApplication extends ConfigApplication {

    private static Supplier<AmazonS3> s3ClientSupplier;

    public static void setS3ClientSupplier(Supplier<AmazonS3> s3ClientSupplier) {
        ConfigIntegrationApplication.s3ClientSupplier = s3ClientSupplier;
    }

    @Override
    public void initialize(Bootstrap<ConfigConfiguration> bootstrap) {
        super.initialize(bootstrap);
    }

    @Override
    protected Module bindS3ConfigSource() {
        return new AbstractModule() {
            @Override
            protected void configure() {
            }

            @Provides
            @Singleton
            @SuppressWarnings("unused")
            private S3ConfigSource getS3ConfigSource(ConfigConfiguration configConfiguration, ObjectMapper objectMapper){
                SelfServiceConfig selfServiceConfig = configConfiguration.getSelfService();
                if (selfServiceConfig.isEnabled()){
                    AmazonS3 amazonS3 = s3ClientSupplier != null ? s3ClientSupplier.get() : AmazonS3ClientBuilder.defaultClient();
                    return new S3ConfigSource(
                            selfServiceConfig,
                            amazonS3,
                            objectMapper);
                }
                return new S3ConfigSource();
            }
        };
    }
}
