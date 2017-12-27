package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class OnboardingForTransactionEntityPredicateTest {

    @Test
    public void shouldBeTrueForNonOnboarding() {
        String transactionEntity = "transactionEntity";
        Predicate<IdentityProviderConfigEntityData> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfigEntityData nonOnboardingIdp = anIdentityProviderConfigData()
                .build();

        assertThat(onboardingPredicate.apply(nonOnboardingIdp)).isTrue();
    }

    @Test
    public void shouldBeTrueForOnboardingForSameTransactionEntity() {
        String transactionEntity = "transactionEntity";
        Predicate<IdentityProviderConfigEntityData> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfigEntityData onboardingSameTransactionEntityIdp = anIdentityProviderConfigData()
                .withOnboarding(ImmutableList.of(transactionEntity))
                .build();

        assertThat(onboardingPredicate.apply(onboardingSameTransactionEntityIdp)).isTrue();
    }

    @Test
    public void shouldBeFalseForOnboardingDifferentTransactionEntity() {
        String transactionEntity = "transactionEntity";
        List<String>  differentTransactionEntity = ImmutableList.of("differentTransactionEntity");
        Predicate<IdentityProviderConfigEntityData> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfigEntityData onboardingDifferentTransactionEntityIdp = anIdentityProviderConfigData()
                .withOnboarding(differentTransactionEntity)
                .build();

        assertThat(onboardingPredicate.apply(onboardingDifferentTransactionEntityIdp)).isFalse();
    }
}
