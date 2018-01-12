package uk.gov.ida.hub.samlsoapproxy.health;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import uk.gov.ida.saml.metadata.MetadataHealthCheck;

import javax.inject.Inject;

public class MetadataHealthCheckRegistry implements Managed {

    private final Environment environment;
    private final MetadataHealthCheck metadataHealthCheck;

    @Inject
    public MetadataHealthCheckRegistry(
            Environment environment,
            MetadataHealthCheck metadataHealthCheck) {
        this.environment = environment;
        this.metadataHealthCheck = metadataHealthCheck;
    }

    @Override
    public void start() throws Exception {
        environment.healthChecks().register("metadataHealthCheck", metadataHealthCheck);
    }

    @Override
    public void stop() throws Exception {
        environment.healthChecks().unregister("metadataHealthCheck");
    }
}
