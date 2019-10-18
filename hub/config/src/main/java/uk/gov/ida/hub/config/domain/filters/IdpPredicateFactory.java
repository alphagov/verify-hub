package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class IdpPredicateFactory {

    @Inject
    public IdpPredicateFactory() {
    }

    @Deprecated
    public Predicate<IdentityProviderConfig> createPredicateForTransactionEntity(Optional<String> transactionEntity) {
        Predicate<IdentityProviderConfig> predicate = IdentityProviderConfig::isEnabled;
        if (transactionEntity.isPresent()) {
            predicate = predicate.and((idpConfig) -> idpConfig.isOnboardingForTransactionEntity(transactionEntity.get()));
        }
        return predicate;
    }

    public Predicate<IdentityProviderConfig> createPredicateForTransactionEntityAndLoa(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return andPredicates(Stream.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig)->idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntity, levelOfAssurance),
                (idpConfig)->idpConfig.supportsLoa(levelOfAssurance),
                IdentityProviderConfig::isRegistrationEnabled));
    }

    public Predicate<IdentityProviderConfig> createPredicateForSignIn(String transactionEntityId) {
        return andPredicates(Stream.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig)->idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null)));
    }

    public Predicate<IdentityProviderConfig> createPredicateForSingleIdp(String transactionEntityId) {
        return andPredicates(Stream.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig)->idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null),
                IdentityProviderConfig::isEnabledForSingleIdp,
                IdentityProviderConfig::isRegistrationEnabled));
    }

    private Predicate<IdentityProviderConfig> andPredicates(Stream<Predicate<IdentityProviderConfig>> predicates){
        return predicates.reduce(Predicate::and).orElseThrow();
    }
}
