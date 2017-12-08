package uk.gov.ida.hub.policy.factories;

import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseContainerDto;
import uk.gov.ida.hub.policy.contracts.SamlAuthnResponseTranslatorDto;

public class SamlAuthnResponseTranslatorDtoFactory {
    public SamlAuthnResponseTranslatorDto fromSamlAuthnResponseContainerDto(SamlAuthnResponseContainerDto samlAuthnResponseContainerDto, String matchingServiceEntityId) {
        return new SamlAuthnResponseTranslatorDto(
                samlAuthnResponseContainerDto.getSamlResponse(),
                samlAuthnResponseContainerDto.getSessionId(),
                samlAuthnResponseContainerDto.getPrincipalIPAddressAsSeenByHub(),
                matchingServiceEntityId);
    }
}
