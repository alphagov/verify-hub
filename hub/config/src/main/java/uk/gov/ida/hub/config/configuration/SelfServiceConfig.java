package uk.gov.ida.hub.config.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Duration;

import javax.validation.Valid;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SelfServiceConfig {

    @Valid
    @JsonProperty
    private boolean enabled = false;

    @Valid
    @JsonProperty
    private String s3BucketName;

    @Valid
    @JsonProperty
    private String s3ObjectKey;

    @Valid
    @JsonProperty
    private Duration cacheExpiry;

    @Valid
    @JsonProperty
    private String s3Endpoint;

    @Valid
    @JsonProperty
    private String s3Region;

    @SuppressWarnings("unused")
    public SelfServiceConfig() { }

    public SelfServiceConfig(boolean enabled){
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    public Duration getCacheExpiry() {
        return cacheExpiry;
    }

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public String getS3Region() {
        return s3Region;
    }
}
