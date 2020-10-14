package uk.gov.ida.bundles;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.configuration.ServiceStatus;
import uk.gov.ida.filters.ConnectionCloseFilter;
import uk.gov.ida.resources.ServiceStatusResource;
import uk.gov.ida.tasks.SetServiceUnavailableTask;

public class ServiceStatusBundle implements ConfiguredBundle<Configuration> {
    @Override
    public void initialize(Bootstrap<?> bootstrap) {}

    @Override
    public void run(Configuration configuration, Environment environment)
            throws Exception {
        environment.jersey().register(new ServiceStatusResource());
        environment.jersey().register(new ConnectionCloseFilter());
        environment.admin().addTask(new SetServiceUnavailableTask(ServiceStatus.getInstance()));
    }
}
