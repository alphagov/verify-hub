package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class MatchingProcess {

    @Valid
    @NotNull
    @JsonProperty
    private Cycle3AttributeName cycle3AttributeName;

    @SuppressWarnings("unused") // needed by jaxb
    private MatchingProcess() {}

    public MatchingProcess(String cycle3AttributeName) {
        this.cycle3AttributeName = Cycle3AttributeName.valueOf(cycle3AttributeName);
    }

    public String getCycle3AttributeName() {
        return cycle3AttributeName.name();
    }
}
