package uk.gov.ida.hub.samlproxy;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.eventemitter.Configuration;

import javax.validation.Valid;

public class EventEmitterConfiguration implements Configuration {

    @Valid
    @JsonProperty
    private String sourceQueueName;

    @Valid
    @JsonProperty
    private String bucketName;

    @Valid
    @JsonProperty
    private String keyName;

    private EventEmitterConfiguration() { }

    public EventEmitterConfiguration(String sourceQueueName) {
        this.sourceQueueName = sourceQueueName;
    }

    @Override
    public String getSourceQueueName() {
        return sourceQueueName;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String getKeyName() {
        return keyName;
    }
}
