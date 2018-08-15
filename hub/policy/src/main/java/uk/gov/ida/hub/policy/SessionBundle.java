package uk.gov.ida.hub.policy;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.ida.hub.policy.controllogic.TransitionStore;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.factories.SessionStoreFactory;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;

import javax.inject.Provider;
import java.util.concurrent.ConcurrentMap;

public class SessionBundle implements ConfiguredBundle<PolicyConfiguration> {

    private static final String EXISTING_SESSION_STORE_ENTRY_COUNT = "existing_session_store_entry_count";
    private static final String TRANSITION_SESSION_STORE_ENTRY_COUNT = "transition_session_store_entry_count";
    private static final String TRANSITION_SESSION_STORE_DIFFERING_KEYS = "transition_session_store_differing_keys";
    private TransitionStore<State> transitionStore;
    private final Provider<InfinispanCacheManager> infinispanCacheManagerProvider;

    public SessionBundle(Provider<InfinispanCacheManager> infinispanCacheManagerProvider) {
        this.infinispanCacheManagerProvider = infinispanCacheManagerProvider;
    }

    @Override
    public void run(PolicyConfiguration configuration, Environment environment) {
        transitionStore = SessionStoreFactory.getSessionStateStore(infinispanCacheManagerProvider.get());

        environment.metrics().register(EXISTING_SESSION_STORE_ENTRY_COUNT, getExistingSizeMetric());
        environment.metrics().register(TRANSITION_SESSION_STORE_ENTRY_COUNT, getTransitionSizeMetric());
        environment.healthChecks().register(TRANSITION_SESSION_STORE_DIFFERING_KEYS, new HealthCheck() {
            @Override
            protected Result check() throws Exception {
                return Result.healthy(transitionStore.getDifferingSessionIds().toString());
            }
        });
    }

    private Gauge<Integer> getExistingSizeMetric() {
        return () -> transitionStore.size();
    }

    private Gauge<Integer> getTransitionSizeMetric() {
        return () -> transitionStore.transitionSize();
    }

    public Provider<ConcurrentMap<SessionId, State>> getSessionStateStoreProvider() {
        return () -> transitionStore;
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {}
}
