package uk.gov.ida.hub.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.eventemitter.Configuration;

import javax.validation.Valid;

public class EventEmitterConfiguration implements Configuration {

    @Valid
    @JsonProperty
    private String sourceQueueName;

    private EventEmitterConfiguration() { }

    public EventEmitterConfiguration(String sourceQueueName) {
        this.sourceQueueName = sourceQueueName;
    }

    @Override
    public String getSourceQueueName() {
        return sourceQueueName;
    }
}
