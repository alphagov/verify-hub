package uk.gov.ida.hub.policy.domain.controller;

import uk.gov.ida.hub.policy.domain.IdpConfigDto;
import uk.gov.ida.hub.policy.domain.LevelOfAssurance;
import uk.gov.ida.hub.policy.domain.exception.StateProcessingValidationException;
import uk.gov.ida.hub.policy.domain.state.IdpSelectedStateTransitional;
import uk.gov.ida.hub.policy.domain.state.IdpSelectingStateTransitional;
import uk.gov.ida.hub.policy.proxy.IdentityProvidersConfigProxy;
import uk.gov.ida.hub.policy.proxy.TransactionsConfigProxy;

import java.util.List;
import java.util.stream.Collectors;

public class IdpSelector {

    private IdpSelector() {}

    public static IdpSelectedStateTransitional buildIdpSelectedState(IdpSelectingStateTransitional state,
                                                                     String idpEntityId,
                                                                     boolean registering,
                                                                     LevelOfAssurance requestedLoa,
                                                                     TransactionsConfigProxy transactionsConfigProxy,
                                                                     IdentityProvidersConfigProxy identityProvidersConfigProxy) {

        List<LevelOfAssurance> levelsOfAssuranceForTransaction = transactionsConfigProxy.getLevelsOfAssurance(state.getRequestIssuerEntityId());
        if (!levelsOfAssuranceForTransaction.contains(requestedLoa)) {
            throw StateProcessingValidationException.requestedLevelOfAssuranceUnsupportedByTransactionEntity(state.getRequestIssuerEntityId(), levelsOfAssuranceForTransaction, requestedLoa);
        }

        List<String> availableIdentityProviderEntityIdsForLoa = identityProvidersConfigProxy.getEnabledIdentityProviders(
                state.getRequestIssuerEntityId(), registering, requestedLoa);

        checkValidIdentityProvider(idpEntityId, availableIdentityProviderEntityIdsForLoa, state);

        IdpConfigDto idpConfig = identityProvidersConfigProxy.getIdpConfig(idpEntityId);
        final List<LevelOfAssurance> idpLevelsOfAssurance = idpConfig.getSupportedLevelsOfAssurance();
        List<LevelOfAssurance> levelsOfAssuranceForTransactionSupportedByIdp = levelsOfAssuranceForTransaction.stream().filter(idpLevelsOfAssurance::contains).collect(Collectors.toList());

        String matchingServiceEntityId = transactionsConfigProxy.getMatchingServiceEntityId(state.getRequestIssuerEntityId());

        return new IdpSelectedStateTransitional(
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
                requestedLoa,
                state.getSessionId(),
                availableIdentityProviderEntityIdsForLoa,
                state.getTransactionSupportsEidas()
        );
    }

    private static void checkValidIdentityProvider(final String idpEntityId, List<String> availableIdentityProviderEntityIdsForLoa, IdpSelectingStateTransitional state) {
        boolean found = false;

        for (String entityId : availableIdentityProviderEntityIdsForLoa) {
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
