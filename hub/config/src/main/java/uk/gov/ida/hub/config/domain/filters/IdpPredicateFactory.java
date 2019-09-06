package uk.gov.ida.hub.config.domain.filters;

import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.Set;

public class IdpPredicateFactory {

    @Inject
    public IdpPredicateFactory() {
    }

    @Deprecated
    public Set<Predicate<IdentityProviderConfig>> createPredicatesForTransactionEntity(Optional<String> transactionEntity) {
        Set<Predicate<IdentityProviderConfig>> predicates = new HashSet<>();
        predicates.add(IdentityProviderConfig::isEnabled);

        Optional.ofNullable(transactionEntity).ifPresent(s -> predicates.add((idpConfig) -> idpConfig.isOnboardingForTransactionEntity(s.get())));

        return predicates;
    }

    public Set<Predicate<IdentityProviderConfig>> createPredicatesForTransactionEntityAndLoa(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        return Set.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig)->idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntity, levelOfAssurance),
                (idpConfig)->idpConfig.supportsLoa(levelOfAssurance),
                IdentityProviderConfig::isRegistrationEnabled);
    }

    public Set<Predicate<IdentityProviderConfig>> createPredicatesForSignIn(String transactionEntityId) {
        return Set.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig)->idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null));
    }

    public Set<Predicate<IdentityProviderConfig>> createPredicatesForSingleIdp(String transactionEntityId) {
        return Set.of(
                IdentityProviderConfig::isEnabled,
                (idpConfig)->idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityId, null),
                IdentityProviderConfig::isEnabledForSingleIdp,
                IdentityProviderConfig::isRegistrationEnabled);
    }
}
