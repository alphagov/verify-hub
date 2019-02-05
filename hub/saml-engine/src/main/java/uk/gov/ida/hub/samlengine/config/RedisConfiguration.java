package uk.gov.ida.hub.samlengine.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.redisson.config.Config;

import javax.validation.Valid;

public class RedisConfiguration extends Config {

    @Valid
    @JsonProperty
    private Long sessionExpiryTimeInMinutes = 150L;

    public Long getSessionExpiryTimeInMinutes() {
        return sessionExpiryTimeInMinutes;
    }
}
