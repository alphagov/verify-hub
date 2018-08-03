package uk.gov.ida.hub.samlproxy;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.eventemitter.Configuration;

import javax.validation.Valid;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEmitterConfiguration implements Configuration {

    @Valid
    @JsonProperty
    private boolean enabled;

    @Valid
    @JsonProperty
    private String queueAccountId;

    @Valid
    @JsonProperty
    private String sourceQueueName;

    @Valid
    @JsonProperty
    private String bucketName;

    @Valid
    @JsonProperty
    private String keyName;

    @Valid
    @JsonProperty
    private String accessKeyId;

    @Valid
    @JsonProperty
    private String secretAccessKey;

    @Valid
    @JsonProperty
    private Regions region;

    private EventEmitterConfiguration() { }

    public EventEmitterConfiguration(String sourceQueueName) {
        this.sourceQueueName = sourceQueueName;
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String getAccessKeyId() {
        return accessKeyId;
    }

    @Override
    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    @Override
    public Regions getRegion() {
        return region;
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

    @Override
    public String getQueueAccountId() {
        return queueAccountId;
    }
}
