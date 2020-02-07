package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public class SessionStoreConfiguration {

    @JsonProperty
    private RedisConfiguration redis;

    public Optional<RedisConfiguration> getRedisConfiguration() {
        return Optional.ofNullable(redis);
    }
}
