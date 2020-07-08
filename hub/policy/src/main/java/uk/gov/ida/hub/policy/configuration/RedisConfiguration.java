package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lettuce.core.RedisURI;

import javax.validation.Valid;
import java.net.URI;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.MINUTES;

public class RedisConfiguration {

    @Valid
    @JsonProperty
    private Duration recordTTL = Duration.of(150, MINUTES);

    @Valid
    @JsonProperty
    private URI uri;

    public Long getRecordTTL() {
        return recordTTL.getSeconds();
    }

    public RedisURI getUri() {
        return RedisURI.Builder.redis(uri.getHost(), uri.getPort()).withTimeout(Duration.ofSeconds(20L)).build();
    }
}
