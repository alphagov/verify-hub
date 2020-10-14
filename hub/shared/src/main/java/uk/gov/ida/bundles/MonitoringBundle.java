package uk.gov.ida.bundles;

import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.configuration.ServiceNameConfiguration;
import uk.gov.ida.resources.ServiceNameResource;
import uk.gov.ida.resources.VersionInfoResource;

public class MonitoringBundle implements ConfiguredBundle<ServiceNameConfiguration> {
    @Override
    public void initialize(Bootstrap<?> bootstrap) {}

    @Override
    public void run(ServiceNameConfiguration configuration, Environment environment)
            throws Exception {
        environment.jersey().register(new ServiceNameResource(configuration.getServiceName()));
        environment.jersey().register(new VersionInfoResource());
    }
}
