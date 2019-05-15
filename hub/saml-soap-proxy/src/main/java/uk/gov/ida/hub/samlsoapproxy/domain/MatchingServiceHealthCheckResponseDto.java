package uk.gov.ida.hub.samlsoapproxy.domain;

import java.util.Optional;

public class MatchingServiceHealthCheckResponseDto {

    private Optional<String> response;

    @SuppressWarnings("unused") //Needed for JAXB
    private MatchingServiceHealthCheckResponseDto() {
    }

    public MatchingServiceHealthCheckResponseDto(Optional<String> response) {
        this.response = response;
    }

    public Optional<String> getResponse() {
        return response;
    }

}
