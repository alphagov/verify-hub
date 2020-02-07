package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionStoreConfiguration {

    @JsonProperty
    private RedisConfiguration redis;

    public RedisConfiguration getRedisConfiguration() {
        return redis;
    }
}
