package uk.gov.ida.hub.policy.domain.state;

import uk.gov.ida.hub.policy.domain.State;

@Deprecated
public class TransitionalStateConverter {

    public static State convertToTransitional(State state) {

        if (state.getClass().equals(SessionStartedState.class)) {
            return getSessionStartedStateTransitional((SessionStartedState) state);
        }

        if (state.getClass().equals(AuthnFailedErrorState.class)) {
            return getAuthnFailedErrorStateTransitional((AuthnFailedErrorState) state);
        }

        if (state.getClass().equals(AwaitingCycle3DataState.class)) {
            return getAwaitingCycle3DataStateTransitional((AwaitingCycle3DataState) state);
        }

        if (state.getClass().equals(FraudEventDetectedState.class)) {
            return getFraudEventDetectedStateTransitional((FraudEventDetectedState) state);
        }

        if (state.getClass().equals(IdpSelectedState.class)) {
            return getIdpSelectedStateTransitional((IdpSelectedState) state);
        }

        if (state.getClass().equals(RequesterErrorState.class)) {
            return getRequesterErrorStateTransitional((RequesterErrorState) state);
        }

        if (state.getClass().equals(SuccessfulMatchState.class)) {
            return getSuccessfulMatchStateTransitional((SuccessfulMatchState) state);
        }

        if (state.getClass().equals(UserAccountCreatedState.class)) {
            return getUserAccountCreatedStateTransitional((UserAccountCreatedState) state);
        }

        return state;
    }

    private static SessionStartedStateTransitional getSessionStartedStateTransitional(SessionStartedState state) {
        return new SessionStartedStateTransitional(
                state.getRequestId(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getForceAuthentication(),
                state.getSessionExpiryTimestamp(),
                state.getSessionId(),
                state.getTransactionSupportsEidas()
        );
    }

    private static AuthnFailedErrorStateTransitional getAuthnFailedErrorStateTransitional(AuthnFailedErrorState state) {
        return new AuthnFailedErrorStateTransitional(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getIdpEntityId(),
                state.getForceAuthentication(),
                state.getTransactionSupportsEidas()
        );
    }

    private static AwaitingCycle3DataStateTransitional getAwaitingCycle3DataStateTransitional(AwaitingCycle3DataState state) {
        return new AwaitingCycle3DataStateTransitional(
                state.getRequestId(),
                state.getIdentityProviderEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getRequestIssuerEntityId(),
                state.getEncryptedMatchingDatasetAssertion(),
                state.getAuthnStatementAssertion(),
                state.getRelayState(),
                state.getAssertionConsumerServiceUri(),
                state.getMatchingServiceEntityId(),
                state.getSessionId(),
                state.getPersistentId(),
                state.getLevelOfAssurance(),
                false,
                state.getTransactionSupportsEidas()
        );
    }

    private static FraudEventDetectedStateTransitional getFraudEventDetectedStateTransitional(FraudEventDetectedState state) {
        return new FraudEventDetectedStateTransitional(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getIdpEntityId(),
                state.getForceAuthentication(),
                state.getTransactionSupportsEidas()
        );
    }

    private static IdpSelectedStateTransitional getIdpSelectedStateTransitional(IdpSelectedState state) {
        return new IdpSelectedStateTransitional(
                state.getRequestId(),
                state.getIdpEntityId(),
                state.getMatchingServiceEntityId(),
                state.getLevelsOfAssurance(),
                state.getUseExactComparisonType(),
                state.getForceAuthentication(),
                state.getAssertionConsumerServiceUri(),
                state.getRequestIssuerEntityId(),
                state.getRelayState(),
                state.getSessionExpiryTimestamp(),
                state.registering(),
                null,
                state.getSessionId(),
                state.getAvailableIdentityProviderEntityIds(),
                state.getTransactionSupportsEidas()
        );
    }

    private static RequesterErrorStateTransitional getRequesterErrorStateTransitional(RequesterErrorState state) {
        return new RequesterErrorStateTransitional(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getRelayState(),
                state.getSessionId(),
                state.getForceAuthentication(),
                state.getTransactionSupportsEidas()
        );
    }

    private static SuccessfulMatchStateTransitional getSuccessfulMatchStateTransitional(SuccessfulMatchState state) {
        return new SuccessfulMatchStateTransitional(
                state.getRequestId(),
                state.getSessionExpiryTimestamp(),
                state.getIdentityProviderEntityId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getRequestIssuerEntityId(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getLevelOfAssurance(),
                false,
                state.getTransactionSupportsEidas()
        );
    }

    private static UserAccountCreatedStateTransitional getUserAccountCreatedStateTransitional(UserAccountCreatedState state) {
        return new UserAccountCreatedStateTransitional(
                state.getRequestId(),
                state.getRequestIssuerEntityId(),
                state.getSessionExpiryTimestamp(),
                state.getAssertionConsumerServiceUri(),
                state.getSessionId(),
                state.getIdentityProviderEntityId(),
                state.getMatchingServiceAssertion(),
                state.getRelayState(),
                state.getLevelOfAssurance(),
                // TT-1613: This will have a value passed properly in the next release
                false,
                state.getTransactionSupportsEidas()
        );
    }
}
