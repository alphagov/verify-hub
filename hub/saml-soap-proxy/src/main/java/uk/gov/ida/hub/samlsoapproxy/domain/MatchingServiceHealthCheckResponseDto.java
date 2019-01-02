package uk.gov.ida.hub.samlsoapproxy.domain;

import java.util.Optional;

public class MatchingServiceHealthCheckResponseDto {

    private Optional<String> response;
    private Optional<String> versionNumber;

    @SuppressWarnings("unused") //Needed for JAXB
    private MatchingServiceHealthCheckResponseDto() {
    }

    public MatchingServiceHealthCheckResponseDto(Optional<String> response, Optional<String> versionNumber) {
        this.response = response;
        this.versionNumber = versionNumber;
    }

    public Optional<String> getResponse() {
        return response;
    }

    public Optional<String> getVersionNumber() {
        return versionNumber;
    }
}
