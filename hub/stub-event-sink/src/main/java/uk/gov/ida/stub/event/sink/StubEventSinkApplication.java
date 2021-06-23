package uk.gov.ida.stub.event.sink;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.hub.shared.guice.GuiceBundle;
import uk.gov.ida.stub.event.sink.healthcheck.StubEventSinkHealthCheck;
import uk.gov.ida.stub.event.sink.resources.EventSinkHubEventResource;
import uk.gov.ida.stub.event.sink.resources.EventSinkHubEventTestResource;

import static java.util.Arrays.asList;

public class StubEventSinkApplication extends Application<StubEventSinkConfiguration> {

    public static void main(String[] args) throws Exception {
        new StubEventSinkApplication().run(args);
    }

    @Override
    public String getName() {
        return "EventSink Service";
    }

    @Override
    public final void initialize(Bootstrap<StubEventSinkConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        GuiceBundle<StubEventSinkConfiguration> guiceBundle = new GuiceBundle<>(
                () -> asList(),
                StubEventSinkConfiguration.class
        );
        bootstrap.addBundle(guiceBundle);
        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
    }

    @Override
    public final void run(StubEventSinkConfiguration configuration, Environment environment) {
        environment.getObjectMapper().setDateFormat(new StdDateFormat());

        StubEventSinkHealthCheck healthCheck = new StubEventSinkHealthCheck();
        environment.healthChecks().register(healthCheck.getName(), healthCheck);

        environment.jersey().register(EventSinkHubEventResource.class);
        environment.jersey().register(EventSinkHubEventTestResource.class);
    }

}
