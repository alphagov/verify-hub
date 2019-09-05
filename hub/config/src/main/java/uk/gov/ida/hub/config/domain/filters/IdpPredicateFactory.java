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
        EnabledIdpPredicate enabledIdpPredicate = new EnabledIdpPredicate();
        Set<Predicate<IdentityProviderConfig>> predicates = new HashSet<>();
        predicates.add(enabledIdpPredicate);

        Optional.ofNullable(transactionEntity).ifPresent(s -> predicates.add(new OnboardingForTransactionEntityPredicate(s.get())));

        return predicates;
    }

    public Set<Predicate<IdentityProviderConfig>> createPredicatesForTransactionEntityAndLoa(String transactionEntity,
                                                                                             LevelOfAssurance levelOfAssurance) {
        return Set.of(new EnabledIdpPredicate(), new OnboardingIdpPredicate(transactionEntity, levelOfAssurance),
                new SupportedLoaIdpPredicate(levelOfAssurance), new NewUserIdpPredicate());
    }

    public Set<Predicate<IdentityProviderConfig>> createPredicatesForSignIn(String transactionEntityId) {
        return Set.of(new EnabledIdpPredicate(), new OnboardingIdpPredicate(transactionEntityId, null));
    }

    public Set<Predicate<IdentityProviderConfig>> createPredicatesForSingleIdp(String transactionEntityId) {
        return Set.of(new EnabledIdpPredicate(), new OnboardingIdpPredicate(transactionEntityId, null), new SingleIdpEnabledPredicate(), new NewUserIdpPredicate());
    }
}
