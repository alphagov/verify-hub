package uk.gov.ida.hub.samlproxy.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.gov.ida.common.SessionId;

import static uk.gov.ida.hub.samlproxy.domain.IdpResult.SUCCESS;

// This annotation is required for ZDD where we may add fields to newer versions of this DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ResponseActionDto {
    private SessionId sessionId;
    private IdpResult result;
    private boolean isRegistration;
    private LevelOfAssurance loaAchieved;

    @SuppressWarnings("unused") // needed by jaxb
    private ResponseActionDto() {
    }

    private ResponseActionDto(SessionId sessionId, IdpResult result, final boolean isRegistration, LevelOfAssurance loaAchieved) {
        this.sessionId = sessionId;
        this.result = result;
        this.isRegistration = isRegistration;
        this.loaAchieved = loaAchieved;
    }

    @JsonIgnore
    public static ResponseActionDto success(SessionId sessionId, boolean registrationContext, LevelOfAssurance loaAchieved) {
        return new ResponseActionDto(sessionId, SUCCESS, registrationContext, loaAchieved);
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    public IdpResult getResult() {
        return result;
    }

    @SuppressWarnings("unused") // needed by jaxb
    public boolean getIsRegistration() {
        return isRegistration;
    }

    public LevelOfAssurance getLoaAchieved() {
        return loaAchieved;
    }
}

