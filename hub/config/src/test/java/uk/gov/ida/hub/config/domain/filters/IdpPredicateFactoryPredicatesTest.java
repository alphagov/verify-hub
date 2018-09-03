package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import org.junit.Test;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.hub.config.domain.filters.PredicateTestHelper.*;

public class IdpPredicateFactoryPredicatesTest {

    private final IdpPredicateFactory idpPredicateFactory = new IdpPredicateFactory();

    @Test
    public void shouldReturnIdpsForLoaForNonOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfigEntityData>> loa1Predicate = idpPredicateFactory
                .createPredicatesForTransactionEntityAndLoa(transactionEntityNonOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa2Idp, onboardingLoa2IdpOtherOnboardingEntity};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForLoaForOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfigEntityData>> loa1PredicateOnboarding = idpPredicateFactory
                .createPredicatesForTransactionEntityAndLoa(transactionEntityOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, loa1PredicateOnboarding);

        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa2IdpOtherOnboardingEntity};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForSignInForNonOnboardingTransactionEntity() {
        final Set<Predicate<IdentityProviderConfigEntityData>> signInPredicateNonOnboarding= idpPredicateFactory
                .createPredicatesForSignIn(transactionEntityNonOnboarding);

        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, signInPredicateNonOnboarding);

        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp,
                nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity, 
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForSignInForOnboardingTransactionEntity()
    {
        final Set<Predicate<IdentityProviderConfigEntityData>> signInPredicateOnboarding = idpPredicateFactory
                .createPredicatesForSignIn(transactionEntityOnboarding);

        final Set<IdentityProviderConfigEntityData> filteredIdps = getFilteredIdps(allIdps, signInPredicateOnboarding);

        final IdentityProviderConfigEntityData[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity, 
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }
}
