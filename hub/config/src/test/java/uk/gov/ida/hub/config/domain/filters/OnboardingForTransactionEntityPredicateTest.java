package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class OnboardingForTransactionEntityPredicateTest {

    @Test
    public void shouldBeTrueForNonOnboarding() {
        String transactionEntity = "transactionEntity";
        Predicate<IdentityProviderConfig> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfig nonOnboardingIdp = anIdentityProviderConfigData()
                .build();

        assertThat(onboardingPredicate.apply(nonOnboardingIdp)).isTrue();
    }

    @Test
    public void shouldBeTrueForOnboardingForSameTransactionEntity() {
        String transactionEntity = "transactionEntity";
        Predicate<IdentityProviderConfig> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfig onboardingSameTransactionEntityIdp = anIdentityProviderConfigData()
                .withOnboarding(ImmutableList.of(transactionEntity))
                .build();

        assertThat(onboardingPredicate.apply(onboardingSameTransactionEntityIdp)).isTrue();
    }

    @Test
    public void shouldBeFalseForOnboardingDifferentTransactionEntity() {
        String transactionEntity = "transactionEntity";
        List<String>  differentTransactionEntity = ImmutableList.of("differentTransactionEntity");
        Predicate<IdentityProviderConfig> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfig onboardingDifferentTransactionEntityIdp = anIdentityProviderConfigData()
                .withOnboarding(differentTransactionEntity)
                .build();

        assertThat(onboardingPredicate.apply(onboardingDifferentTransactionEntityIdp)).isFalse();
    }
}
