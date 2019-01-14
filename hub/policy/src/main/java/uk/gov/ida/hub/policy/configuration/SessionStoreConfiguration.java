package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;
import uk.gov.ida.shared.dropwizard.infinispan.config.CacheType;
import uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanConfiguration;

import java.util.Optional;

import static com.google.common.base.Optional.absent;

public class SessionStoreConfiguration {

    @JsonProperty
    private InfinispanConfiguration infinispan = new InfinispanConfiguration(
            absent(), -1, absent(), absent(), CacheType.standalone, com.google.common.base.Optional.of(Duration.hours(2)),
            absent(), absent(), absent(), absent()
    );

    @JsonProperty
    private RedisConfiguration redis;

    public InfinispanConfiguration getInfinispanConfiguration() {
        return infinispan;
    }

    public Optional<RedisConfiguration> getRedisConfiguration() {
        return Optional.ofNullable(redis);
    }
}
