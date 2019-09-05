package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

public class OnboardingForTransactionEntityPredicateTest {

    @Test
    public void shouldBeTrueForNonOnboarding() {
        String transactionEntity = "transactionEntity";
        Predicate<IdentityProviderConfig> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfig nonOnboardingIdp = anIdentityProviderConfigData()
                .build();

        assertThat(onboardingPredicate.test(nonOnboardingIdp)).isTrue();
    }

    @Test
    public void shouldBeTrueForOnboardingForSameTransactionEntity() {
        String transactionEntity = "transactionEntity";
        Predicate<IdentityProviderConfig> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfig onboardingSameTransactionEntityIdp = anIdentityProviderConfigData()
                .withOnboarding(List.of(transactionEntity))
                .build();

        assertThat(onboardingPredicate.test(onboardingSameTransactionEntityIdp)).isTrue();
    }

    @Test
    public void shouldBeFalseForOnboardingDifferentTransactionEntity() {
        String transactionEntity = "transactionEntity";
        List<String> differentTransactionEntity = List.of("differentTransactionEntity");
        Predicate<IdentityProviderConfig> onboardingPredicate = new OnboardingForTransactionEntityPredicate(transactionEntity);

        IdentityProviderConfig onboardingDifferentTransactionEntityIdp = anIdentityProviderConfigData()
                .withOnboarding(differentTransactionEntity)
                .build();

        assertThat(onboardingPredicate.test(onboardingDifferentTransactionEntityIdp)).isFalse();
    }
}
