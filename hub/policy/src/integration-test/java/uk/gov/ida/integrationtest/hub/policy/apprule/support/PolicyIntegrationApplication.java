package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import uk.gov.ida.hub.policy.PolicyApplication;
import uk.gov.ida.hub.policy.PolicyConfiguration;
import uk.gov.ida.hub.policy.PolicyModule;
import uk.gov.ida.hub.policy.SessionModule;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;

import javax.inject.Provider;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

public class PolicyIntegrationApplication extends PolicyApplication {

    private final InfinispanCacheManager infinispanCacheManager =
            new InfinispanCacheManager(mock(MetricRegistry.class, RETURNS_MOCKS), TestCacheManagerFactory.createCacheManager());;

    @Override
    protected void registerResources(PolicyConfiguration configuration, Environment environment) {
        super.registerResources(configuration, environment);
        environment.jersey().register(TestSessionResource.class);
    }

    @Override
    protected PolicyModule getPolicyModule() {
        return new PolicyModuleForIntegrationTests();
    }

    @Override
    protected SessionModule getSessionModule(Provider<InfinispanCacheManager> infinispanCacheManagerProvider,
                         Provider<ConcurrentMap<SessionId, State>> sessionStateStoreProvider) {
        return new SessionModule(() -> infinispanCacheManager, () -> getDataStore());
    }

    public ConcurrentMap<SessionId,State> getDataStore() {
        return infinispanCacheManager.getCache("state_cache");
    }

    private static class PolicyModuleForIntegrationTests extends PolicyModule {
        @Override
        protected void configure() {
            bind(TestSessionRepository.class);
            super.configure();
        }
    }
}
