package uk.gov.ida.hub.policy;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;

import javax.inject.Provider;
import java.util.concurrent.ConcurrentMap;

public class SessionModule extends AbstractModule {
    private final Provider<InfinispanCacheManager> infinispanCacheManagerProvider;
    private final Provider<ConcurrentMap<SessionId, State>> sessionStateStoreProvider;

    public SessionModule(Provider<InfinispanCacheManager> infinispanCacheManagerProvider,
                         Provider<ConcurrentMap<SessionId, State>> sessionStateStoreProvider) {
        this.infinispanCacheManagerProvider = infinispanCacheManagerProvider;
        this.sessionStateStoreProvider = sessionStateStoreProvider;
    }

    @Override
    protected void configure() {
        bind(InfinispanCacheManager.class).toProvider(infinispanCacheManagerProvider);
        bind(new TypeLiteral<ConcurrentMap<SessionId, State>>() {}).toProvider(sessionStateStoreProvider);
    }
}
