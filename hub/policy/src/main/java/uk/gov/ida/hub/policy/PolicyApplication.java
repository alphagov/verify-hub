package uk.gov.ida.hub.policy;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import uk.gov.ida.bundles.LoggingBundle;
import uk.gov.ida.bundles.MonitoringBundle;
import uk.gov.ida.bundles.ServiceStatusBundle;
import uk.gov.ida.eventemitter.EventEmitterModule;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.domain.exception.SessionAlreadyExistingExceptionMapper;
import uk.gov.ida.hub.policy.domain.exception.SessionCreationFailureExceptionMapper;
import uk.gov.ida.hub.policy.domain.exception.SessionNotFoundExceptionMapper;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationExceptionMapper;
import uk.gov.ida.hub.policy.exception.IdaJsonProcessingExceptionMapperBundle;
import uk.gov.ida.hub.policy.exception.IdpDisabledExceptionMapper;
import uk.gov.ida.hub.policy.exception.InvalidSessionStateExceptionMapper;
import uk.gov.ida.hub.policy.exception.PolicyApplicationExceptionMapper;
import uk.gov.ida.hub.policy.exception.SessionTimeoutExceptionMapper;
import uk.gov.ida.hub.policy.filters.SessionIdPathParamLoggingFilter;
import uk.gov.ida.hub.policy.resources.AuthnRequestFromTransactionResource;
import uk.gov.ida.hub.policy.resources.Cycle3DataResource;
import uk.gov.ida.hub.policy.resources.MatchingServiceFailureResponseResource;
import uk.gov.ida.hub.policy.resources.MatchingServiceResponseResource;
import uk.gov.ida.hub.policy.resources.ResponseFromIdpResource;
import uk.gov.ida.hub.policy.resources.SessionResource;
import uk.gov.ida.metrics.bundle.PrometheusBundle;

public class PolicyApplication extends Application<PolicyConfiguration> {

    public static void main(String[] args) throws Exception {
        new PolicyApplication().run(args);
    }

    @Override
    public String getName() {
        return "Policy Service";
    }

    @Override
    public final void initialize(Bootstrap<PolicyConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addBundle(new ServiceStatusBundle());
        bootstrap.addBundle(new MonitoringBundle());
        bootstrap.addBundle(new LoggingBundle());
        bootstrap.addBundle(new PrometheusBundle());
        bootstrap.addBundle(new IdaJsonProcessingExceptionMapperBundle());
        bootstrap.addBundle(
                GuiceBundle.builder().enableAutoConfig(getClass().getPackage().getName())
                        .modules(
                                getPolicyModule(),
                                new EventEmitterModule()
                        )
                        .build()
        );
    }

    protected PolicyModule getPolicyModule() {
        return new PolicyModule();
    }

    @Override
    public void run(PolicyConfiguration configuration, Environment environment) {
        environment.getObjectMapper().setDateFormat(new StdDateFormat());
        registerResources(configuration, environment);
        environment.jersey().register(SessionIdPathParamLoggingFilter.class);
    }

    protected void registerResources(PolicyConfiguration configuration, Environment environment) {
        environment.jersey().register(AuthnRequestFromTransactionResource.class);
        environment.jersey().register(ResponseFromIdpResource.class);
        environment.jersey().register(Cycle3DataResource.class);
        environment.jersey().register(SessionResource.class);
        environment.jersey().register(MatchingServiceResponseResource.class);
        environment.jersey().register(MatchingServiceFailureResponseResource.class);
    }
}
