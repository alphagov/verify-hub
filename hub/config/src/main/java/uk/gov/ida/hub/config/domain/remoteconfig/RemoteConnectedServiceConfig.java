package uk.gov.ida.hub.config.domain.remoteconfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

public class RemoteConnectedServiceConfig {

    @JsonProperty
    protected String entityId;

    @JsonProperty
    protected RemoteServiceProviderConfig serviceProviderConfig;

    @JsonProperty
    protected RemoteMatchingServiceConfig matchingServiceConfig;

    @SuppressWarnings("unused")
    protected RemoteConnectedServiceConfig() {
    }



}
