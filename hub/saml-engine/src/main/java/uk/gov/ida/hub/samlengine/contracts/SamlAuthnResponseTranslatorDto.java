package uk.gov.ida.hub.samlengine.contracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.ida.common.SessionId;

import javax.validation.constraints.NotNull;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class SamlAuthnResponseTranslatorDto {
    private String samlResponse;
    private SessionId sessionId;
    private String principalIPAddressAsSeenByHub;

    @NotNull
    private String matchingServiceEntityId;

    @SuppressWarnings("unused") //Needed for JAXB
    private SamlAuthnResponseTranslatorDto() {
    }


    public SamlAuthnResponseTranslatorDto(String samlResponse, SessionId sessionId, String principalIPAddressAsSeenByHub, String matchingServiceEntityId) {
        this.samlResponse = samlResponse;
        this.sessionId = sessionId;
        this.principalIPAddressAsSeenByHub = principalIPAddressAsSeenByHub;
        this.matchingServiceEntityId = matchingServiceEntityId;
    }

    public String getSamlResponse() {
        return samlResponse;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public String getPrincipalIPAddressAsSeenByHub() { return principalIPAddressAsSeenByHub; }

    public String getMatchingServiceEntityId() {
        return matchingServiceEntityId;
    }
}
