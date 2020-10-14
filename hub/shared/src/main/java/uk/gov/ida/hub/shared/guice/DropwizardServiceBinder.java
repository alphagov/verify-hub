package uk.gov.ida.hub.shared.guice;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.spi.ProvisionListener;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ExceptionMapper;

public class DropwizardServiceBinder implements ProvisionListener {
    private static final Logger LOG = LoggerFactory.getLogger(DropwizardServiceBinder.class);

    private final Environment environment;

    public DropwizardServiceBinder(Environment environment) {
        this.environment = environment;
    }

    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        Object obj = provision.provision();

        if (obj instanceof Managed) {
            handle((Managed) obj);
        }

        if (obj instanceof Task) {
            handle((Task) obj);
        }

        if (obj instanceof HealthCheck) {
            handle((HealthCheck) obj);
        }

        if (obj instanceof ServerLifecycleListener) {
            handle((ServerLifecycleListener) obj);
        }

        if (obj instanceof ExceptionMapper) {
            LOG.info("onProvision ExceptionMapper: {}", provision.getBinding());
            environment.jersey().register(obj);
        }
    }

    private void handle(ServerLifecycleListener obj) {
        environment.lifecycle().addServerLifecycleListener(obj);
    }

    private void handle(Managed obj) {
        environment.lifecycle().manage(obj);
    }

    private void handle(Task obj) {
        environment.admin().addTask(obj);
    }

    private void handle(HealthCheck obj) {
        environment.healthChecks().register(obj.getClass().getSimpleName(), obj);
    }
}
