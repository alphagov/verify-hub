package uk.gov.ida.integrationtest.hub.policy.builders;

import com.codahale.metrics.MetricRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import uk.gov.ida.shared.dropwizard.infinispan.util.InfinispanCacheManager;

import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;

public class InfinispanCacheManagerBuilder {


    public static InfinispanCacheManagerBuilder anInfinispanCacheManager(){
        return new InfinispanCacheManagerBuilder();
    }

    public InfinispanCacheManager build(EmbeddedCacheManager embeddedCacheManager){
        return new InfinispanCacheManager(mock(MetricRegistry.class, RETURNS_MOCKS), embeddedCacheManager);
    }

}
