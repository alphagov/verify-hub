package uk.gov.ida.hub.config;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import engineering.reliability.gds.metrics.bundle.PrometheusBundle;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.common.shared.security.TrustStoreMetrics;
import uk.gov.ida.hub.config.configuration.SelfServiceConfig;
import uk.gov.ida.hub.config.data.S3ConfigSource;
import uk.gov.ida.hub.config.filters.SessionIdQueryParamLoggingFilter;
import uk.gov.ida.hub.config.resources.CertificatesResource;
import uk.gov.ida.hub.config.resources.CountriesResource;
import uk.gov.ida.hub.config.resources.IdentityProviderResource;
import uk.gov.ida.hub.config.resources.MatchingServiceResource;
import uk.gov.ida.hub.config.resources.TransactionsResource;
import uk.gov.ida.truststore.ClientTrustStoreConfiguration;
import uk.gov.ida.truststore.KeyStoreLoader;

import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import java.security.KeyStore;
import java.util.EnumSet;

public class ConfigApplication extends Application<ConfigConfiguration> {

    private GuiceBundle<ConfigConfiguration> guiceBundle;

    public static void main(String[] args) throws Exception {
        new ConfigApplication().run(args);
    }

    @Override
    public String getName() {
        return "Config Service";
    }

    @Override
    public void initialize(Bootstrap<ConfigConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        guiceBundle = GuiceBundle.defaultBuilder(ConfigConfiguration.class)
                .modules(new ConfigModule())
                .modules(bindS3ConfigSource())
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
        bootstrap.addBundle(new PrometheusBundle());
        bootstrap.addCommand(new ConfigValidCommand());
    }

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
                    return new S3ConfigSource(
                            selfServiceConfig,
                            AmazonS3ClientBuilder.defaultClient(),
                            objectMapper);
                }
                return new S3ConfigSource();
            }
        };
    }

    @Override
    public void run(ConfigConfiguration configuration, Environment environment) {
        environment.getObjectMapper().setDateFormat(new StdDateFormat());
        registerResources(environment);
        environment.servlets().addFilter("Logging SessionId registration Filter", SessionIdQueryParamLoggingFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

        TrustStoreMetrics trustStoreMetrics = new TrustStoreMetrics();
        registerMetrics(trustStoreMetrics, "rp", configuration.getRpTrustStoreConfiguration());
        registerMetrics(trustStoreMetrics, "client", configuration.getClientTrustStoreConfiguration());
    }

    private void registerMetrics(TrustStoreMetrics metrics, String trustStoreName, ClientTrustStoreConfiguration trustStoreConfiguration) {
        KeyStore trustStore = new KeyStoreLoader().load(trustStoreConfiguration.getPath(), trustStoreConfiguration.getPassword());
        metrics.registerTrustStore(trustStoreName, trustStore);
    }

    private void registerResources(Environment environment) {
        environment.jersey().register(CertificatesResource.class);
        environment.jersey().register(IdentityProviderResource.class);
        environment.jersey().register(TransactionsResource.class);
        environment.jersey().register(MatchingServiceResource.class);
        environment.jersey().register(CountriesResource.class);
    }
}
