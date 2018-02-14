package uk.gov.ida.hub.samlsoapproxy;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.hub.samlsoapproxy.exceptions.IdaJsonProcessingExceptionMapperBundle;
import uk.gov.ida.hub.samlsoapproxy.filters.SessionIdQueryParamLoggingFilter;
import uk.gov.ida.hub.samlsoapproxy.resources.AttributeQueryRequestSenderResource;
import uk.gov.ida.hub.samlsoapproxy.resources.MatchingServiceHealthCheckResource;
import uk.gov.ida.hub.samlsoapproxy.resources.MatchingServiceVersionCheckResource;
import uk.gov.ida.saml.core.IdaSamlBootstrap;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

import static com.hubspot.dropwizard.guicier.GuiceBundle.defaultBuilder;

public class SamlSoapProxyApplication extends Application<SamlSoapProxyConfiguration> {

    private GuiceBundle<SamlSoapProxyConfiguration> guiceBundle;

    public static void main(String[] args) throws Exception {
        new SamlSoapProxyApplication().run(args);
    }

    @Override
    public String getName() {
        return "SamlSoapProxy Service";
    }


    @Override
    public final void initialize(Bootstrap<SamlSoapProxyConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addBundle(new IdaJsonProcessingExceptionMapperBundle());
        guiceBundle = defaultBuilder(SamlSoapProxyConfiguration.class)
                .modules(new SamlSoapProxyModule())
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
    }

    @Override
    public void run(SamlSoapProxyConfiguration configuration, Environment environment) throws Exception {
        IdaSamlBootstrap.bootstrap();
        environment.getObjectMapper().setDateFormat(new ISO8601DateFormat());
        registerResources(environment);
        environment.servlets().addFilter("Logging SessionId registration Filter", SessionIdQueryParamLoggingFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    private void registerResources(Environment environment) {
        environment.jersey().register(AttributeQueryRequestSenderResource.class);
        environment.jersey().register(MatchingServiceHealthCheckResource.class);
        environment.jersey().register(MatchingServiceVersionCheckResource.class);
    }
}
