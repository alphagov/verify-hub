package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.MatchingProcess;

public class MatchingProcessBuilder {

    private String cycle3AttributeName;

    public static MatchingProcessBuilder aMatchingProcess() {
        return new MatchingProcessBuilder();
    }

    public MatchingProcess build() {
        return new MatchingProcess(cycle3AttributeName);
    }

    public MatchingProcessBuilder withCycle3AttributeName(String attributeName) {
        this.cycle3AttributeName = attributeName;
        return this;
    }
}
