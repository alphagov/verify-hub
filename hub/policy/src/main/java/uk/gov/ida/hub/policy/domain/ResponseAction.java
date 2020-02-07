package uk.gov.ida.hub.policy.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.CANCEL;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.FAILED_UPLIFT;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.MATCHING_JOURNEY_SUCCESS;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.NON_MATCHING_JOURNEY_SUCCESS;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.OTHER;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.PENDING;
import static uk.gov.ida.hub.policy.domain.ResponseAction.IdpResult.SUCCESS;

// We would like to use the following annotation so in future we can re-use ResponseActionDto,
// however, it appears Infinispan does not like it: (@JsonIgnoreProperties(ignoreUnknown = true))
// TODO: now that infinispan has gone, can we re-enable this?
public final class ResponseAction {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseAction.class);

    public enum IdpResult {
        SUCCESS, MATCHING_JOURNEY_SUCCESS, NON_MATCHING_JOURNEY_SUCCESS, CANCEL, FAILED_UPLIFT, PENDING, OTHER
    }
    
    private SessionId sessionId;
    private IdpResult result;
    private boolean isRegistration;
    private LevelOfAssurance loaAchieved;
    private String notOnOrAfter;

    @JsonIgnore
    public static ResponseAction cancel(SessionId sessionId, boolean isRegistration) { return new ResponseAction(sessionId, CANCEL, isRegistration, null, null); }

    @JsonIgnore
    public static ResponseAction other(SessionId sessionId, boolean isRegistration) {
        LOG.info(format("Response action 'OTHER' created for session id '{0}'", sessionId));
        return new ResponseAction(sessionId, OTHER, isRegistration, null, null);
    }

    @JsonIgnore
    public static ResponseAction failedUplift(SessionId sessionId, boolean isRegistration) { return new ResponseAction(sessionId, FAILED_UPLIFT, isRegistration, null, null); }

    @JsonIgnore
    public static ResponseAction success(SessionId sessionId, boolean isRegistration, LevelOfAssurance loaAchieved, String notOnOrAfter) { return new ResponseAction(sessionId, SUCCESS, isRegistration, loaAchieved, notOnOrAfter); }

    @JsonIgnore
    public static ResponseAction matchingJourneySuccess(SessionId sessionId, boolean isRegistration, LevelOfAssurance loaAchieved, String notOnOrAfter) { return new ResponseAction(sessionId, MATCHING_JOURNEY_SUCCESS, isRegistration, loaAchieved, notOnOrAfter); }

    @JsonIgnore
    public static ResponseAction nonMatchingJourneySuccess(SessionId sessionId, boolean isRegistration, LevelOfAssurance loaAchieved, String notOnOrAfter) { return new ResponseAction(sessionId, NON_MATCHING_JOURNEY_SUCCESS, isRegistration, loaAchieved, notOnOrAfter); }

    @JsonIgnore
    public static ResponseAction pending(SessionId sessionId) { return new ResponseAction(sessionId, PENDING, true, null, null); }

    @SuppressWarnings("unused")//Needed by JAXB
    private ResponseAction() {
    }

    private ResponseAction(SessionId sessionId, IdpResult result, boolean isRegistration, LevelOfAssurance loaAchieved, String notOnOrAfter) {
        this.sessionId = sessionId;
        this.result = result;
        this.isRegistration = isRegistration;
        this.loaAchieved = loaAchieved;
        this.notOnOrAfter = notOnOrAfter;
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

    public String getNotOnOrAfter() {
        return notOnOrAfter;
    }
}
