package uk.gov.ida.integrationtest.hub.samlengine.builders;

import uk.gov.ida.common.SessionId;
import uk.gov.ida.hub.samlengine.contracts.SamlAuthnResponseTranslatorDto;
import uk.gov.ida.shared.utils.string.StringEncoding;

public class SamlAuthnResponseTranslatorDtoBuilder {

    private String samlResponse = StringEncoding.toBase64Encoded("blah");
    private SessionId sessionId = SessionId.createNewSessionId();
    private String principalIPAddressAsSeenByHub = "NOT SET IN BUILDER";
    private String matchingServiceEntityId = null;


    public static SamlAuthnResponseTranslatorDtoBuilder aSamlAuthnResponseTranslatorDto() {
        return new SamlAuthnResponseTranslatorDtoBuilder();
    }

    public SamlAuthnResponseTranslatorDto build() {
        return new SamlAuthnResponseTranslatorDto(samlResponse, sessionId, principalIPAddressAsSeenByHub, matchingServiceEntityId);
    }

    public SamlAuthnResponseTranslatorDtoBuilder withSamlResponse(String samlAuthnResponse) {
        this.samlResponse = samlAuthnResponse;
        return this;
    }

    public SamlAuthnResponseTranslatorDtoBuilder withMatchingServiceEntityId(String entityId) {
        this.matchingServiceEntityId = entityId;
        return this;
    }

}
