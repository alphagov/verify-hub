package uk.gov.ida.hub.samlsoapproxy.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;

//Unused fields can be removed once they are removed from the app config for all environments.
public class SamlConfiguration {
    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    @Valid
    @JsonProperty
    protected URI expectedDestination;

    public String getEntityId() {
        return entityId;
    }

    public URI getExpectedDestinationHost() {
        return expectedDestination;
    }

}
