package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

public class SessionStoreConfiguration {

    @Valid
    @JsonProperty
    protected InfinispanConfiguration infinispanConfiguration;

    public InfinispanConfiguration getInfinispanConfiguration() {
        return infinispanConfiguration;
    }
}
