package uk.gov.ida.hub.samlsoapproxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ExecutorConfiguration {

    protected ExecutorConfiguration() {
    }

    @Valid
    @NotNull
    @JsonProperty
    protected Integer corePoolSize;

    @Valid
    @NotNull
    @JsonProperty
    protected Integer maxPoolSize;

    @Valid
    @NotNull
    @JsonProperty
    protected Duration keepAliveDuration;

    public Integer getCorePoolSize() {
        return corePoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public Duration getKeepAliveDuration() {
        return keepAliveDuration;
    }
}
