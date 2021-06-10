package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import io.dropwizard.setup.Environment;
import uk.gov.ida.hub.policy.PolicyApplication;
import uk.gov.ida.hub.policy.configuration.PolicyConfiguration;
import uk.gov.ida.hub.policy.PolicyModule;

public class PolicyIntegrationApplication extends PolicyApplication {

    @Override
    protected void registerResources(PolicyConfiguration configuration, Environment environment) {
        super.registerResources(configuration, environment);
        environment.jersey().register(TestSessionResource.class);
    }

    @Override
    protected PolicyModule getPolicyModule() {
        return new PolicyModuleForIntegrationTests();
    }

    private static class PolicyModuleForIntegrationTests extends PolicyModule {
        @Override
        protected void configure() {
            bind(TestSessionRepository.class);
            super.configure();
        }
    }
}
