package uk.gov.ida.hub.config.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SelfServiceConfig {

    @Valid
    @JsonProperty
    private boolean enabled = false;

    @JsonProperty
    private String awsRegion;

    @Valid
    @JsonProperty
    private String s3BucketName;

    @Valid
    @JsonProperty
    private String s3ObjectKey;

    @Valid
    @JsonProperty
    private String s3AccessKeyId;

    @Valid
    @JsonProperty
    private String s3SecretKeyId;

    @JsonProperty
    private long cacheLengthInSeconds = 10;

    @SuppressWarnings("unused")
    public SelfServiceConfig() { }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getS3BucketName() {
        return s3BucketName;
    }

    public String getS3AccessKeyId() {
        return s3AccessKeyId;
    }

    public String getS3SecretKeyId() {
        return s3SecretKeyId;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    public long getCacheLengthInSeconds() {
        return cacheLengthInSeconds;
    }
}
