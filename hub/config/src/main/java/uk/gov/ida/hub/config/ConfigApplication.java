package uk.gov.ida.hub.config;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.hub.config.filters.SessionIdQueryParamLoggingFilter;
import uk.gov.ida.hub.config.resources.CertificatesResource;
import uk.gov.ida.hub.config.resources.CountriesResource;
import uk.gov.ida.hub.config.resources.IdentityProviderResource;
import uk.gov.ida.hub.config.resources.MatchingServiceResource;
import uk.gov.ida.hub.config.resources.TransactionsResource;

import javax.servlet.DispatcherType;
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
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
        bootstrap.addCommand(new ConfigValidCommand());
    }

    @Override
    public void run(ConfigConfiguration configuration, Environment environment) {
        environment.getObjectMapper().setDateFormat(new StdDateFormat());
        registerResources(environment);
        environment.servlets().addFilter("Logging SessionId registration Filter", SessionIdQueryParamLoggingFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    private void registerResources(Environment environment) {
        environment.jersey().register(CertificatesResource.class);
        environment.jersey().register(IdentityProviderResource.class);
        environment.jersey().register(TransactionsResource.class);
        environment.jersey().register(MatchingServiceResource.class);
        environment.jersey().register(CountriesResource.class);
    }
}
