package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lettuce.core.RedisURI;

import javax.validation.Valid;
import java.net.URI;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

public class RedisConfiguration {

    @Valid
    @JsonProperty
    private Duration recordTTL = Duration.of(150, MINUTES);

    @Valid
    @JsonProperty
    private URI uri;

    @Valid
    @JsonProperty
    private Duration timeout = Duration.of(20L, SECONDS);

    public Long getRecordTTL() {
        return recordTTL.getSeconds();
    }

    public RedisURI getUri() {
        return RedisURI.create(uri);
    }

    public Duration getTimeout() {
        return timeout;
    }
}
