package uk.gov.ida.integrationtest.hub.samlengine.support;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.dropwizard.setup.Environment;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import uk.gov.ida.hub.samlengine.SamlEngineApplication;
import uk.gov.ida.hub.samlengine.SamlEngineConfiguration;
import uk.gov.ida.integrationtest.hub.samlengine.resources.TestSamlMessageResource;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;

import javax.inject.Provider;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

public class SamlEngineIntegrationApplication extends SamlEngineApplication {

    @Override
    protected void registerResources(Environment environment, SamlEngineConfiguration configuration) {
        super.registerResources(environment, configuration);
        environment.jersey().register(TestSamlMessageResource.class);
    }

    @Override
    protected Module bindInfinispan(Provider<InfinispanCacheManager> ignored) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(InfinispanCacheManager.class).toProvider((javax.inject.Provider<InfinispanCacheManager>) () -> new InfinispanCacheManager(mock(MetricRegistry.class, RETURNS_MOCKS), TestCacheManagerFactory.createCacheManager()));
            }
        };
    }

}
