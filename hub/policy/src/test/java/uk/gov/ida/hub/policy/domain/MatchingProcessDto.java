package uk.gov.ida.hub.policy.domain;

import com.google.common.base.Optional;

public class MatchingProcessDto {

    private Optional<String> attributeName = Optional.absent();

    @SuppressWarnings("unused") // needed by jaxb
    private MatchingProcessDto() {}

    public MatchingProcessDto(Optional<String> attributeName) {
        this.attributeName = attributeName;
    }

    public Optional<String> getAttributeName() {
        return attributeName;
    }
}
