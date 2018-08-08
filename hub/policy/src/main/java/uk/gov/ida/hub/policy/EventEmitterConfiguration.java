package uk.gov.ida.hub.policy;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.eventemitter.Configuration;

import javax.validation.Valid;
import java.util.Base64;

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
    private String accessKeyId;

    @Valid
    @JsonProperty
    private String secretAccessKey;

    @Valid
    @JsonProperty
    private Regions region;

    @Valid
    @JsonProperty
    private String encryptionKey;

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
    public String getQueueAccountId() {
        return queueAccountId;
    }

    @Override
    public byte[] getEncryptionKey() {
        return Base64.getDecoder().decode(encryptionKey);
    }
}
