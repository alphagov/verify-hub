package uk.gov.ida.hub.policy.domain;

import java.util.Optional;

public class MatchingProcessDto {

    private Optional<String> attributeName = Optional.empty();

    @SuppressWarnings("unused") // needed by jaxb
    private MatchingProcessDto() {}

    public MatchingProcessDto(Optional<String> attributeName) {
        this.attributeName = attributeName;
    }

    public Optional<String> getAttributeName() {
        return attributeName;
    }
}
