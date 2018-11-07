package uk.gov.ida.hub.policy.domain;

import uk.gov.ida.hub.policy.domain.state.AuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.AwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasAuthnFailedErrorState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectedState;
import uk.gov.ida.hub.policy.domain.state.EidasCountrySelectingState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.Cycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.Cycle3DataInputCancelledState;
import uk.gov.ida.hub.policy.domain.state.Cycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasAwaitingCycle3DataState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle0And1MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasCycle3MatchRequestSentState;
import uk.gov.ida.hub.policy.domain.state.EidasSuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.EidasUserAccountCreationRequestSentState;
import uk.gov.ida.hub.policy.domain.state.FraudEventDetectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.MatchingServiceRequestErrorState;
import uk.gov.ida.hub.policy.domain.state.NoMatchState;
import uk.gov.ida.hub.policy.domain.state.NonMatchingRequestReceivedState;
import uk.gov.ida.hub.policy.domain.state.RequesterErrorState;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;
import uk.gov.ida.hub.policy.domain.state.SuccessfulMatchState;
import uk.gov.ida.hub.policy.domain.state.TimeoutState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreatedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationFailedState;
import uk.gov.ida.hub.policy.domain.state.UserAccountCreationRequestSentState;

import static java.text.MessageFormat.format;
import static java.util.Arrays.stream;

public enum PolicyState {
    SESSION_STARTED(SessionStartedState.class),
    EIDAS_COUNTRY_SELECTING(EidasCountrySelectingState.class),
    EIDAS_COUNTRY_SELECTED(EidasCountrySelectedState.class),
    IDP_SELECTED(IdpSelectedState.class),
    NON_MATCHING_REQUEST_RECEIVED(NonMatchingRequestReceivedState.class),
    CYCLE_0_AND_1_MATCH_REQUEST_SENT(Cycle0And1MatchRequestSentState.class),
    EIDAS_CYCLE_0_AND_1_MATCH_REQUEST_SENT(EidasCycle0And1MatchRequestSentState.class),
    EIDAS_CYCLE_3_MATCH_REQUEST_SENT(EidasCycle3MatchRequestSentState.class),
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
    EIDAS_USER_ACCOUNT_CREATION_REQUEST_SENT(EidasUserAccountCreationRequestSentState.class),
    AUTHN_FAILED_ERROR(AuthnFailedErrorState.class),
    EIDAS_AUTHN_FAILED_ERROR(EidasAuthnFailedErrorState.class),
    FRAUD_EVENT_DETECTED(FraudEventDetectedState.class),
    REQUESTER_ERROR(RequesterErrorState.class),
    CYCLE_3_DATA_INPUT_CANCELLED(Cycle3DataInputCancelledState.class),
    USER_ACCOUNT_CREATION_FAILED(UserAccountCreationFailedState.class),
    EIDAS_USER_ACCOUNT_CREATION_FAILED(EidasUserAccountCreationFailedState .class);

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
