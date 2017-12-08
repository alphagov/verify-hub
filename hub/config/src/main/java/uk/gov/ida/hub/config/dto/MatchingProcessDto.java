package uk.gov.ida.hub.config.dto;

import java.util.Optional;

public class MatchingProcessDto {

    private String attributeName;

    @SuppressWarnings("unused") // needed by jaxb
    private MatchingProcessDto() {}

    public MatchingProcessDto(String attributeName) {
        this.attributeName = attributeName;
    }

    public Optional<String> getAttributeName() {
        return Optional.ofNullable(attributeName);
    }
}
