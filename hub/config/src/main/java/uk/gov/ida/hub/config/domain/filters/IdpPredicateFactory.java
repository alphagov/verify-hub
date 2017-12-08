package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class IdpPredicateFactory {

    @Inject
    public IdpPredicateFactory() {
    }

    @Deprecated
    public Set<Predicate<IdentityProviderConfigEntityData>> createPredicatesForTransactionEntity(Optional<String> transactionEntity) {
        EnabledIdpPredicate enabledIdpPredicate = new EnabledIdpPredicate();
        Set<Predicate<IdentityProviderConfigEntityData>> predicates = new HashSet<>();
        predicates.add(enabledIdpPredicate);

        Optional.ofNullable(transactionEntity).ifPresent(s -> predicates.add(new OnboardingForTransactionEntityPredicate(s.get())));

        return predicates;
    }

    public Set<Predicate<IdentityProviderConfigEntityData>> createPredicatesForTransactionEntity(String transactionEntity,
                                                                                                 LevelOfAssurance levelOfAssurance) {
        return Sets.newHashSet(new EnabledIdpPredicate(), new OnboardingIdpPredicate(transactionEntity, levelOfAssurance));
    }

    public Set<Predicate<IdentityProviderConfigEntityData>> createPredicatesForSignIn(String transactionEntityId) {
        return createPredicatesForTransactionEntity(transactionEntityId, null);
    }
}
