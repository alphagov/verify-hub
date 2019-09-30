package uk.gov.ida.hub.samlsoapproxy.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class PrometheusClientServiceConfiguration {
    @NotNull
    @Valid
    @JsonProperty
    private Boolean enable = false;

    @NotNull
    @Valid
    @JsonProperty
    private Duration initialDelay = Duration.seconds(10L);

    @NotNull
    @Valid
    @JsonProperty
    private Duration delay =  Duration.minutes(1L);

    @NotNull
    @Valid
    @JsonProperty
    private Integer minNumOfThreads = 0;

    @NotNull
    @Valid
    @JsonProperty
    private Integer maxNumOfThreads = 50;

    @NotNull
    @Valid
    @JsonProperty
    private Duration keepAliveTime = Duration.seconds(60L);

    public PrometheusClientServiceConfiguration() { }

    public Boolean getEnable() {
        return enable;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getDelay() {
        return delay;
    }

    public Integer getMinNumOfThreads() {
        return minNumOfThreads;
    }

    public Integer getMaxNumOfThreads() {
        return maxNumOfThreads;
    }

    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }
}
