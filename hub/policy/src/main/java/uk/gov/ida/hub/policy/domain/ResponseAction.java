package uk.gov.ida.hub.policy.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;

import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.CANCEL;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.FAILED_UPLIFT;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.MATCHING_JOURNEY_SUCCESS;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.NON_MATCHING_JOURNEY_SUCCESS;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.OTHER;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.PENDING;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.SUCCESS;

// We would like to use the following annotation so in future we can re-use ResponseActionDto,
// however, it appears Infinispan does not like it: (@JsonIgnoreProperties(ignoreUnknown = true))
public final class ResponseAction {
    public enum IdpResult {
        SUCCESS, MATCHING_JOURNEY_SUCCESS, NON_MATCHING_JOURNEY_SUCCESS, CANCEL, FAILED_UPLIFT, PENDING, OTHER
    }
    
    private SessionId sessionId;
    private IdpResult result;
    private boolean isRegistration;
    private LevelOfAssurance loaAchieved;

    @JsonIgnore
    public static ResponseAction cancel(SessionId sessionId, boolean isRegistration) { return new ResponseAction(sessionId, CANCEL, isRegistration, null); }

    @JsonIgnore
    public static ResponseAction other(SessionId sessionId, boolean isRegistration) { return new ResponseAction(sessionId, OTHER, isRegistration, null); }

    @JsonIgnore
    public static ResponseAction failedUplift(SessionId sessionId, boolean isRegistration) { return new ResponseAction(sessionId, FAILED_UPLIFT, isRegistration, null); }

    @JsonIgnore
    public static ResponseAction success(SessionId sessionId, boolean isRegistration, LevelOfAssurance loaAchieved) { return new ResponseAction(sessionId, SUCCESS, isRegistration, loaAchieved); }

    @JsonIgnore
    public static ResponseAction matchingJourneySuccess(SessionId sessionId, boolean isRegistration, LevelOfAssurance loaAchieved) { return new ResponseAction(sessionId, MATCHING_JOURNEY_SUCCESS, isRegistration, loaAchieved); }

    @JsonIgnore
    public static ResponseAction nonMatchingJourneySuccess(SessionId sessionId, boolean isRegistration, LevelOfAssurance loaAchieved) { return new ResponseAction(sessionId, NON_MATCHING_JOURNEY_SUCCESS, isRegistration, loaAchieved); }

    @JsonIgnore
    public static ResponseAction pending(SessionId sessionId) { return new ResponseAction(sessionId, PENDING, true, null); }

    @SuppressWarnings("unused")//Needed by JAXB
    private ResponseAction() {
    }

    private ResponseAction(SessionId sessionId, IdpResult result, boolean isRegistration, LevelOfAssurance loaAchieved) {
        this.sessionId = sessionId;
        this.result = result;
        this.isRegistration = isRegistration;
        this.loaAchieved = loaAchieved;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    @SuppressWarnings("unused")//Needed by JAXB
    public IdpResult getResult() {
        return result;
    }

    @SuppressWarnings("unused")//Needed by JAXB
    public boolean getIsRegistration() {
        return isRegistration;
    }

    public LevelOfAssurance getLoaAchieved() {
        return loaAchieved;
    }
}
