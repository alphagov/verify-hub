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
    SESSION_STARTED(SessionStartedState.class),                                             // Deprecated
    SESSION_STARTED_TRANSITIONAL(SessionStartedStateTransitional.class),
    COUNTRY_SELECTING(CountrySelectingState.class),
    COUNTRY_SELECTED(CountrySelectedState.class),
    IDP_SELECTED(IdpSelectedState.class),                                                   // Deprecated
    IDP_SELECTED_TRANSITIONAL(IdpSelectedStateTransitional.class),
    CYCLE_0_AND_1_MATCH_REQUEST_SENT(Cycle0And1MatchRequestSentState.class),                // Deprecated
    CYCLE_0_AND_1_MATCH_REQUEST_SENT_TRANSITIONAL(Cycle0And1MatchRequestSentStateTransitional.class),
    EIDAS_CYCLE_0_AND_1_MATCH_REQUEST_SENT(EidasCycle0And1MatchRequestSentState.class),
    SUCCESSFUL_MATCH(SuccessfulMatchState.class),                                           // Deprecated
    SUCCESSFUL_MATCH_TRANSITIONAL(SuccessfulMatchStateTransitional.class),
    EIDAS_SUCCESSFUL_MATCH(EidasSuccessfulMatchState.class),
    NO_MATCH(NoMatchState.class),
    USER_ACCOUNT_CREATED(UserAccountCreatedState.class),                                    // Deprecated
    USER_ACCOUNT_CREATED_TRANSITIONAL(UserAccountCreatedStateTransitional.class),
    AWAITING_CYCLE3_DATA(AwaitingCycle3DataState.class),                                    // Deprecated
    AWAITING_CYCLE3_DATA_TRANSITIONAL(AwaitingCycle3DataStateTransitional.class),
    EIDAS_AWAITING_CYCLE3_DATA(EidasAwaitingCycle3DataState.class),
    CYCLE3_MATCH_REQUEST_SENT(Cycle3MatchRequestSentState.class),                           // Deprecated
    CYCLE3_MATCH_REQUEST_SENT_TRANSITIONAL(Cycle3MatchRequestSentStateTransitional.class),
    TIMEOUT(TimeoutState.class),
    MATCHING_SERVICE_REQUEST_ERROR(MatchingServiceRequestErrorState.class),
    USER_ACCOUNT_CREATION_REQUEST_SENT(UserAccountCreationRequestSentState.class),          // Deprecated
    USER_ACCOUNT_CREATION_REQUEST_SENT_TRANSITIONAL(UserAccountCreationRequestSentStateTransitional.class),
    AUTHN_FAILED_ERROR(AuthnFailedErrorState.class),                                        // Deprecated
    AUTHN_FAILED_ERROR_TRANSITIONAL(AuthnFailedErrorStateTransitional.class),
    FRAUD_EVENT_DETECTED(FraudEventDetectedState.class),                                    // Deprecated
    FRAUD_EVENT_DETECTED_TRANSITIONAL(FraudEventDetectedStateTransitional.class),
    REQUESTER_ERROR(RequesterErrorState.class),                                             // Deprecated
    REQUESTER_ERROR_TRANSITIONAL(RequesterErrorStateTransitional.class),
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
