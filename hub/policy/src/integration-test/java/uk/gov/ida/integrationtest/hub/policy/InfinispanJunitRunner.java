package uk.gov.ida.integrationtest.hub.policy;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

public class InfinispanJunitRunner extends MockitoJUnitRunner {

    public static final EmbeddedCacheManager EMBEDDED_CACHE_MANAGER = TestCacheManagerFactory.createCacheManager();
    private static boolean initialized = false;

    public InfinispanJunitRunner(Class<?> klass) throws InitializationError, InvocationTargetException {
        super(klass);
        if (!initialized) {
            setInitialized();
        }
    }

    private static synchronized void setInitialized() {
        initialized = true;
    }

    @Override
    public void run(final RunNotifier notifier) {
        notifier.addListener(new RunListener() {
            @Override
            public void testStarted(Description description) throws Exception {
                super.testStarted(description);
            }

            @Override
            public void testFinished(Description description) throws Exception {
                super.testFinished(description);
                EMBEDDED_CACHE_MANAGER.getCache("state_cache").clear();
                EMBEDDED_CACHE_MANAGER.getCache("assertion_id_cache").clear();
            }
        });
        super.run(notifier);
    }
}
