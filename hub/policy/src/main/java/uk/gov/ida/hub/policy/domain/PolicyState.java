package uk.gov.ida.hub.policy.domain;

import uk.gov.ida.hub.policy.domain.state.*;

import static java.text.MessageFormat.format;
import static java.util.Arrays.stream;

public enum PolicyState {
    SESSION_STARTED(SessionStartedState.class),
    COUNTRY_SELECTING(CountrySelectingState.class),
    COUNTRY_SELECTED(CountrySelectedState.class),
    IDP_SELECTED(IdpSelectedState.class),
    CYCLE_0_AND_1_MATCH_REQUEST_SENT(Cycle0And1MatchRequestSentState.class),
    EIDAS_CYCLE_0_AND_1_MATCH_REQUEST_SENT(EidasCycle0And1MatchRequestSentState.class),
    SUCCESSFUL_MATCH(SuccessfulMatchState.class),
    EIDAS_SUCCESSFUL_MATCH(EidasSuccessfulMatchState.class),
    NO_MATCH(NoMatchState.class),
    USER_ACCOUNT_CREATED(UserAccountCreatedState.class),
    AWAITING_CYCLE3_DATA(AwaitingCycle3DataState.class),
    EIDAS_AWAITING_CYCLE3_DATA(EidasAwaitingCycle3DataState.class),
    CYCLE3_MATCH_REQUEST_SENT(Cycle3MatchRequestSentState.class),
    TIMEOUT(TimeoutState.class),
    MATCHING_SERVICE_REQUEST_ERROR(MatchingServiceRequestErrorState.class),
    USER_ACCOUNT_CREATION_REQUEST_SENT(UserAccountCreationRequestSentState.class),
    AUTHN_FAILED_ERROR(AuthnFailedErrorState.class),
    FRAUD_EVENT_DETECTED(FraudEventDetectedState.class),
    REQUESTER_ERROR(RequesterErrorState.class),
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
