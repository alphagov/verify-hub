package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

public class RemoteConnectedServiceConfig {

    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    @Valid
    @NotNull
    @JsonProperty
    protected RemoteServiceProviderConfig serviceProviderConfig;

    @Valid
    @NotNull
    @JsonProperty
    protected Optional<RemoteMatchingServiceConfig> matchingServiceConfig;

    @SuppressWarnings("unused")
    protected RemoteConnectedServiceConfig() {
    }



}
