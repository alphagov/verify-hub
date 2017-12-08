package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;

public class MatchingProcess {

    private Optional<String> attributeName;

    @SuppressWarnings("unused")//Needed by JAXB
    private MatchingProcess() {
    }

    public MatchingProcess(Optional<String> attributeName) {
        this.attributeName = attributeName;
    }

    public Optional<String> getAttributeName() {
        return attributeName;
    }
}
