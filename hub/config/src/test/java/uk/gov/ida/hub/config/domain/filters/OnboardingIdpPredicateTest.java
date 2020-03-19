package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.Set;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.allIdps;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.getFilteredIdps;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.nonOnboardingAllLevelsIdp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.nonOnboardingHardDisconnectingIdp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.nonOnboardingLoa1Idp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.nonOnboardingLoa2Idp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.nonOnboardingSoftDisconnectingIdp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingAllLevelsIdp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingHardDisconnectingIdp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingLoa1Idp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingLoa1IdpOtherOnboardingEntity;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingLoa2Idp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingLoa2IdpOtherOnboardingEntity;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.onboardingSoftDisconnectingIdp;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.transactionEntityNonOnboarding;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.transactionEntityOnboarding;

public class OnboardingIdpPredicateTest {

    @Test
    public void shouldReturnIdpForLoaForNonOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> loa1Predicate = (idpConfig) -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityNonOnboarding, LevelOfAssurance.LEVEL_1);
        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        // Doesn't need to contain the onboardingSoftDisconnectingIdp or onboardingHardDisconnectingIdp because these IDPs onboard at all levels,
        // meaning that the second check in the OnboardingIdpPredicate's apply function will be evaluated (to false), excluding the IDP from the
        // result set.
        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa2Idp, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing,
                nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpForLoaForOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> loa1PredicateOnboarding = (idpConfig) -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityOnboarding, LevelOfAssurance.LEVEL_1);
        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1PredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa2IdpOtherOnboardingEntity,
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp, nonOnboardingSoftDisconnectingIdp,
                nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsWithoutLoaForNonOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> signInPredicateNonOnboarding = (idpConfig) -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityNonOnboarding, null);
        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, signInPredicateNonOnboarding);

        // Doesn't need to contain the onboardingSoftDisconnectingIdp or onboardingHardDisconnectingIdp because these IDPs onboard at all levels,
        // meaning that the second check in the OnboardingIdpPredicate's apply function will be evaluated (to false), excluding the IDP from the
        // result set.
        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsWithoutLoaForOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> signInPredicateOnboarding = (idpConfig) -> idpConfig.isOnboardingForTransactionEntityAtLoa(transactionEntityOnboarding, null);
        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, signInPredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }
}
