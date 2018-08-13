package uk.gov.ida.hub.policy.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

public class InfinispanConfiguration {

    @Valid
    @JsonProperty
    protected boolean enabled;

    @Valid
    @JsonProperty
    private uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanConfiguration configuration;

    public boolean isEnabled() {
        return enabled;
    }

    public uk.gov.ida.shared.dropwizard.infinispan.config.InfinispanConfiguration getConfiguration() {
        return configuration;
    }
}
