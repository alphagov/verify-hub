package uk.gov.ida.hub.config.domain.filters;

import org.joda.time.Duration;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

public class IdpPredicateFactory {

    private final Duration sessionDuration;

    @Inject
    public IdpPredicateFactory(@Named("userHubSessionDuration") Duration userHubSessionDuration) {
        this.sessionDuration = userHubSessionDuration;
    }

    public Predicate<IdentityProviderConfig> createPredicateForIdpsDisconnectedForRegistration(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return andPredicates(Arrays.asList(
                createPredicateForTransactionEntityAndLoa(transactionEntity, levelOfAssurance),
                disconnectedForRegistrationPredicate()
        ));
    }

    public Predicate<IdentityProviderConfig> createPredicateForSendingRegistrationRequest(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return andPredicates(Arrays.asList(
                createPredicateForTransactionEntityAndLoa(transactionEntity, levelOfAssurance),
                registrationPredicate(false)
        ));
    }

    public Predicate<IdentityProviderConfig> createPredicateForReceivingRegistrationResponse(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return andPredicates(Arrays.asList(
                createPredicateForTransactionEntityAndLoa(transactionEntity, levelOfAssurance),
                registrationPredicate(true)
        ));
    }

    public Predicate<IdentityProviderConfig> createPredicateForSignIn(String transactionEntityId) {
        return andPredicates(Arrays.asList(
                IdentityProviderConfig::isEnabled,
                (idpConfig) -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null)
        ));
    }

    public Predicate<IdentityProviderConfig> createPredicateForSingleIdp(String transactionEntityId) {
        return andPredicates(Arrays.asList(
                IdentityProviderConfig::isEnabled,
                idpConfig -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null),
                IdentityProviderConfig::isEnabledForSingleIdp,
                registrationPredicate(false)
        ));
    }

    private Predicate<IdentityProviderConfig> createPredicateForTransactionEntityAndLoa(
            String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return andPredicates(Arrays.asList(
                IdentityProviderConfig::isEnabled,
                idpConfig -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntity, levelOfAssurance),
                idpConfig -> idpConfig.supportsLoa(levelOfAssurance)
        ));
    }

    private Predicate<IdentityProviderConfig> registrationPredicate(boolean processingIdpResponse) {
        return processingIdpResponse ?
                idpConfig -> idpConfig.canSendRegistrationResponses(sessionDuration) :
                IdentityProviderConfig::canReceiveRegistrationRequests;
    }

    private Predicate<IdentityProviderConfig> disconnectedForRegistrationPredicate() {
        return idpConfig -> !idpConfig.canReceiveRegistrationRequests();
    }

    private Predicate<IdentityProviderConfig> andPredicates(Collection<Predicate<IdentityProviderConfig>> predicates) {
        return predicates.stream().reduce(Predicate::and).orElseThrow();
    }
}
