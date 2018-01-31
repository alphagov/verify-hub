package uk.gov.ida.hub.policy.domain;

import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataStateTransitional;
import uk.gov.ida.hub.policy.domain.state.CountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.CountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentStateTransitional;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchStateTransitional;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentStateTransitional;

import static java.text.MessageFormat.format;
import static java.util.Arrays.stream;

public enum PolicyState {
    SESSION_STARTED(SessionStartedStateTransitional.class),
    SESSION_STARTED_OLD(SessionStartedState.class),                                             // Deprecated
    COUNTRY_SELECTING(CountrySelectingState.class),
    COUNTRY_SELECTED(CountrySelectedState.class),
    IDP_SELECTED(IdpSelectedStateTransitional.class),
    IDP_SELECTED_OLD(IdpSelectedState.class),                                                   // Deprecated
    CYCLE_0_AND_1_MATCH_REQUEST_SENT(Cycle0And1MatchRequestSentStateTransitional.class),
    CYCLE_0_AND_1_MATCH_REQUEST_SENT_OLD(Cycle0And1MatchRequestSentState.class),                // Deprecated
    EIDAS_CYCLE_0_AND_1_MATCH_REQUEST_SENT(EidasCycle0And1MatchRequestSentState.class),
    SUCCESSFUL_MATCH(SuccessfulMatchStateTransitional.class),
    SUCCESSFUL_MATCH_OLD(SuccessfulMatchState.class),                                           // Deprecated
    EIDAS_SUCCESSFUL_MATCH(EidasSuccessfulMatchState.class),
    NO_MATCH(NoMatchState.class),
    USER_ACCOUNT_CREATED(UserAccountCreatedStateTransitional.class),
    USER_ACCOUNT_CREATED_OLD(UserAccountCreatedState.class),                                    // Deprecated
    AWAITING_CYCLE3_DATA(AwaitingCycle3DataStateTransitional.class),
    AWAITING_CYCLE3_DATA_OLD(AwaitingCycle3DataState.class),                                    // Deprecated
    EIDAS_AWAITING_CYCLE3_DATA(EidasAwaitingCycle3DataState.class),
    CYCLE3_MATCH_REQUEST_SENT(Cycle3MatchRequestSentStateTransitional.class),
    CYCLE3_MATCH_REQUEST_SENT_OLD(Cycle3MatchRequestSentState.class),                           // Deprecated
    TIMEOUT(TimeoutState.class),
    MATCHING_SERVICE_REQUEST_ERROR(MatchingServiceRequestErrorState.class),
    USER_ACCOUNT_CREATION_REQUEST_SENT(UserAccountCreationRequestSentStateTransitional.class),
    USER_ACCOUNT_CREATION_REQUEST_SENT_OLD(UserAccountCreationRequestSentState.class),          // Deprecated
    AUTHN_FAILED_ERROR(AuthnFailedErrorStateTransitional.class),
    AUTHN_FAILED_ERROR_OLD(AuthnFailedErrorState.class),                                        // Deprecated
    FRAUD_EVENT_DETECTED(FraudEventDetectedStateTransitional.class),
    FRAUD_EVENT_DETECTED_OLD(FraudEventDetectedState.class),                                    // Deprecated
    REQUESTER_ERROR(RequesterErrorStateTransitional.class),
    REQUESTER_ERROR_OLD(RequesterErrorState.class),                                             // Deprecated
    CYCLE_3_DATA_INPUT_CANCELLED(Cycle3DataInputCancelledState.class),
    USER_ACCOUNT_CREATION_FAILED(UserAccountCreationFailedState.class);

    private final Class<? extends State> stateClass;

    PolicyState(Class<? extends State> stateClass) {
        this.stateClass = stateClass;
    }

    public static PolicyState fromStateClass(Class<? extends State> stateClass) {
        return stream(values())
            .filter(x -> x.stateClass.equals(stateClass))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(format("Unable to locate state for {0}", stateClass.getSimpleName())));
    }
}
