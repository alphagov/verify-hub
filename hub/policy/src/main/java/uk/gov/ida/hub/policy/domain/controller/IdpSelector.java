package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedState;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingState;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.List;
import java.util.stream.Collectors;

public class IdpSelector {

    private IdpSelector() {}

    public static IdpSelectedState buildIdpSelectedState(IdpSelectingState state,
                                                         String idpEntityId,
                                                         boolean registering,
                                                         TransactionsConfigProxy transactionsConfigProxy,
                                                         IdentityProvidersConfigProxy identityProvidersConfigProxy) {
        checkValidIdentityProvider(idpEntityId, state);

        List<LevelOfAssurance> levelsOfAssuranceForTransaction = transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId());
        IdpConfigDto idpConfig = identityProvidersConfigProxy.getIdpConfig(idpEntityId);
        final List<LevelOfAssurance> idpLevelsOfAssurance = idpConfig.getSupportedLevelsOfAssurance();
        List<LevelOfAssurance> levelsOfAssuranceForTransactionSupportedByIdp = levelsOfAssuranceForTransaction.stream().filter(idpLevelsOfAssurance::contains).collect(Collectors.toList());

        if (levelsOfAssuranceForTransactionSupportedByIdp.isEmpty()) {
            throw StateProcessingValidationException.transactionLevelsOfAssuranceUnsupportedByIDP(state.getRequestIssuerEntityId(), levelsOfAssuranceForTransaction, idpEntityId, idpLevelsOfAssurance);
        }

        String matchingServiceEntityId = transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId());
        IdpSelectedState idpSelectedState = new IdpSelectedState(
                state.getRequestId(),
                idpEntityId,
                matchingServiceEntityId,
                levelsOfAssuranceForTransactionSupportedByIdp,
                idpConfig.getUseExactComparisonType(),
                state.getForceAuthentication(),
                state.getAssertionConsumerServiceUri(),
                state.getRequestIssuerEntityId(),
                state.getRelayState(),
                state.getSessionExpiryTimestamp(),
                registering,
                state.getSessionId(),
                state.getAvailableIdentityProviderEntityIds(),
                state.getTransactionSupportsEidas()
        );
        return idpSelectedState;
    }

    private static void checkValidIdentityProvider(final String idpEntityId, IdpSelectingState state) {
        boolean found = false;

        for (String entityId : state.getAvailableIdentityProviderEntityIds()) {
            if (entityId.equals(idpEntityId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw StateProcessingValidationException.unavailableIdp(idpEntityId, state.getSessionId());
        }
    }
}
