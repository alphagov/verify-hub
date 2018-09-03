package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.*;

public class OnboardingIdpPredicateTest {

    @Test
    public void shouldReturnIdpForLoaForNonOnboardingTransactionEntity() {
        final OnboardingIdpPredicate loa1Predicate = new OnboardingIdpPredicate(transactionEntityNonOnboarding, LevelOfAssurance.LEVEL_1);
        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        // Doesn't need to contain the onboardingSoftDisconnectingIdp or onboardingHardDisconnectingIdp because these IDPs onboard at all levels,
        // meaning that the second check in the OnboardingIdpPredicate's apply function will be evaluated (to false), excluding the IDP from the
        // result set.
        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa2Idp, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpForLoaForOnboardingTransactionEntity() {
        final OnboardingIdpPredicate loa1PredicateOnboarding = new OnboardingIdpPredicate(transactionEntityOnboarding, LevelOfAssurance.LEVEL_1);
        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, loa1PredicateOnboarding);

        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa2IdpOtherOnboardingEntity,
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp, nonOnboardingSoftDisconnectingIdp,
                nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsWithoutLoaForNonOnboardingTransactionEntity()
    {
        final OnboardingIdpPredicate signInPredicateNonOnboarding = new OnboardingIdpPredicate(transactionEntityNonOnboarding, null);
        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, signInPredicateNonOnboarding);

        // Doesn't need to contain the onboardingSoftDisconnectingIdp or onboardingHardDisconnectingIdp because these IDPs onboard at all levels,
        // meaning that the second check in the OnboardingIdpPredicate's apply function will be evaluated (to false), excluding the IDP from the
        // result set.
        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsWithoutLoaForOnboardingTransactionEntity()
    {
        final OnboardingIdpPredicate signInPredicateOnboarding = new OnboardingIdpPredicate(transactionEntityOnboarding, null);
        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, signInPredicateOnboarding);

        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

}
