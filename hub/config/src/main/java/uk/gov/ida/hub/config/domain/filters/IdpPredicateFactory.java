package uk.gov.ida.hub.config.domain.filters;

import org.joda.time.Duration;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IdpPredicateFactory {

    private final Duration sessionDuration;

    @Inject
    public IdpPredicateFactory(@Named("userHubSessionDuration") Duration userHubSessionDuration) {
        this.sessionDuration = userHubSessionDuration;
    }

    public Predicate<IdentityProviderConfig> createPredicateForSendingRegistrationRequest(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return createPredicateForTransactionEntityAndLoa(transactionEntity, levelOfAssurance, false);
    }

    public Predicate<IdentityProviderConfig> createPredicateForReceivingRegistrationResponse(
            String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return createPredicateForTransactionEntityAndLoa(transactionEntity, levelOfAssurance, true);
    }

    public Predicate<IdentityProviderConfig> createPredicateForSignIn(String transactionEntityId) {
        return andPredicates(Stream.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig) -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null)));
    }

    public Predicate<IdentityProviderConfig> createPredicateForSingleIdp(String transactionEntityId) {
        return andPredicates(Stream.of(
                IdentityProviderConfig::isEnabled,
                idpConfig -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null),
                IdentityProviderConfig::isEnabledForSingleIdp,
                registrationPredicate(false)));
    }

    private Predicate<IdentityProviderConfig> createPredicateForTransactionEntityAndLoa(
            String transactionEntity, LevelOfAssurance levelOfAssurance, boolean processingIdpResponse) {
        return andPredicates(Stream.of(
                IdentityProviderConfig::isEnabled,
                idpConfig -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntity, levelOfAssurance),
                idpConfig -> idpConfig.supportsLoa(levelOfAssurance),
                registrationPredicate(processingIdpResponse)));
    }

    private Predicate<IdentityProviderConfig> registrationPredicate(boolean processingIdpResponse) {
        return processingIdpResponse ?
                idpConfig -> idpConfig.canSendRegistrationResponses(sessionDuration) :
                IdentityProviderConfig::canReceiveRegistrationRequests;
    }

    private Predicate<IdentityProviderConfig> andPredicates(Stream<Predicate<IdentityProviderConfig>> predicates) {
        return predicates.reduce(Predicate::and).orElseThrow();
    }
}
