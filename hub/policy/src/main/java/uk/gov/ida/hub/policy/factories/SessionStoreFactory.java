package uk.gov.ida.hub.policy.factories;

import org.joda.time.DateTime;
import uk.gov.ida.hub.policy.controllogic.TransitionStore;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

public class SessionStoreFactory {

    private SessionStoreFactory() {}

    public static TransitionStore<State> getSessionStateStore(
            InfinispanCacheManager infinispanCacheManager
    ) {
        return getSessionStore(infinispanCacheManager, "state_cache");
    }

    public static ConcurrentMap<SessionId, DateTime> getSessionExpirationStore(
            InfinispanCacheManager infinispanCacheManager
    ) {
        return getSessionStore(infinispanCacheManager, "datetime_cache");
    }

    private static <V> TransitionStore<V> getSessionStore(InfinispanCacheManager infinispanCacheManager, String storeName) {
        ConcurrentMap<SessionId, V> infinispan = infinispanCacheManager.getCache(storeName);
        return new TransitionStore<>(infinispan, Optional.empty());
    }
}
