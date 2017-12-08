package uk.gov.ida.integrationtest.hub.policy.builders;

import uk.gov.ida.hub.policy.contracts.SamlResponseDto;

import java.util.UUID;

public class SamlResponseDtoBuilder {

    private String samlMessage = UUID.randomUUID().toString();

    public static SamlResponseDtoBuilder anAttributeQueryResponse() {
        return new SamlResponseDtoBuilder();
    }

    public SamlResponseDto build() {
        return new SamlResponseDto(samlMessage);
    }
}
