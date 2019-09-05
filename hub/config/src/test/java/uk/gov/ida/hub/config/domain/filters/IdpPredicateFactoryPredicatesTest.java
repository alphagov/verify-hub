package uk.gov.ida.hub.config.domain.filters;

import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.function.Predicate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.*;

public class IdpPredicateFactoryPredicatesTest {

    private final IdpPredicateFactory idpPredicateFactory = new IdpPredicateFactory();

    @Test
    public void shouldReturnIdpsForLoaForNonOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfig>> loa1Predicate = idpPredicateFactory
                .createPredicatesForTransactionEntityAndLoa(transactionEntityNonOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa2Idp, onboardingLoa2IdpOtherOnboardingEntity};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForLoaForOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfig>> loa1PredicateOnboarding = idpPredicateFactory
                .createPredicatesForTransactionEntityAndLoa(transactionEntityOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1PredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa2IdpOtherOnboardingEntity};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForSignInForNonOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfig>> signInPredicateNonOnboarding = idpPredicateFactory
                .createPredicatesForSignIn(transactionEntityNonOnboarding);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, signInPredicateNonOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp,
                nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForSignInForOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfig>> signInPredicateOnboarding = idpPredicateFactory
                .createPredicatesForSignIn(transactionEntityOnboarding);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, signInPredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }
}
