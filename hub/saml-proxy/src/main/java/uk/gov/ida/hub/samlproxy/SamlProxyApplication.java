package uk.gov.ida.hub.samlproxy;

import com.fasterxml.jackson.databind.util.StdDateFormat;
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
import uk.gov.ida.eventemitter.EventEmitterModule;
import uk.gov.ida.hub.samlproxy.exceptions.NoKeyConfiguredForEntityExceptionMapper;
import uk.gov.ida.hub.samlproxy.exceptions.SamlProxyApplicationExceptionMapper;
import uk.gov.ida.hub.samlproxy.exceptions.SamlProxyExceptionMapper;
import uk.gov.ida.hub.samlproxy.exceptions.SamlProxySamlTransformationErrorExceptionMapper;
import uk.gov.ida.hub.samlproxy.filters.SessionIdQueryParamLoggingFilter;
import uk.gov.ida.hub.samlproxy.resources.HubMetadataResourceApi;
import uk.gov.ida.hub.samlproxy.resources.SamlMessageReceiverApi;
import uk.gov.ida.hub.samlproxy.resources.SamlMessageSenderApi;
import uk.gov.ida.saml.core.IdaSamlBootstrap;

import javax.servlet.DispatcherType;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.hubspot.dropwizard.guicier.GuiceBundle.defaultBuilder;

public class SamlProxyApplication extends Application<SamlProxyConfiguration> {

    private GuiceBundle<SamlProxyConfiguration> guiceBundle;

    public static void main(String[] args) throws Exception {
        new SamlProxyApplication().run(args);
    }

    @Override
    public String getName() {
        return "SamlProxy Service";
    }

    @Override
    public final void initialize(Bootstrap<SamlProxyConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        guiceBundle = defaultBuilder(SamlProxyConfiguration.class)
                .modules(new SamlProxyModule(), new EventEmitterModule())
                .build();
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
        bootstrap.addBundle(new PrometheusBundle());
    }

    @Override
    public void run(SamlProxyConfiguration configuration, Environment environment) throws Exception {
        environment.getObjectMapper().setDateFormat(new StdDateFormat());

        IdaSamlBootstrap.bootstrap();

        for (Class klass : getResources()) {
            environment.jersey().register(klass);
        }

        for (Class klass : getExceptionMappers()) {
            environment.jersey().register(klass);
        }

        environment.servlets().addFilter("Logging SessionId registration Filter", SessionIdQueryParamLoggingFilter.class).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    public List<Class<?>> getResources() {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(SamlMessageReceiverApi.class);
        classes.add(SamlMessageSenderApi.class);
        classes.add(HubMetadataResourceApi.class);
        return classes;
    }

    public List<Class<? extends ExceptionMapper<?>>> getExceptionMappers() {
        List<Class<? extends ExceptionMapper<?>>> classes = new ArrayList<>();
        classes.add(NoKeyConfiguredForEntityExceptionMapper.class);
        classes.add(SamlProxySamlTransformationErrorExceptionMapper.class);
        classes.add(SamlProxyApplicationExceptionMapper.class);
        classes.add(SamlProxyExceptionMapper.class);
        return classes;
    }
}
