package uk.gov.ida.hub.config.domain.filters;

import org.joda.time.Duration;
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

public class IdpPredicateFactoryPredicatesTest {

    private final IdpPredicateFactory idpPredicateFactory = new IdpPredicateFactory(new Duration(90 * 60 * 1000));

    @Test
    public void shouldReturnIdpsEnabledForAuthnRequestForLoaForNonOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> loa1Predicate = idpPredicateFactory
                .createPredicateForSendingRegistrationRequest(transactionEntityNonOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa2Idp, onboardingLoa2IdpOtherOnboardingEntity};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsEnabledForResponseProcessingForLoaForNonOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> loa1Predicate = idpPredicateFactory
                .createPredicateForReceivingRegistrationResponse(transactionEntityNonOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1Predicate);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa2Idp, onboardingLoa2IdpOtherOnboardingEntity, nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsEnabledForAuthnRequestForLoaForOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> loa1PredicateOnboarding = idpPredicateFactory
                .createPredicateForSendingRegistrationRequest(transactionEntityOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1PredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa2IdpOtherOnboardingEntity};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsEnabledForResponseProcessingForLoaForOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> loa1PredicateOnboarding = idpPredicateFactory
                .createPredicateForReceivingRegistrationResponse(transactionEntityOnboarding, LevelOfAssurance.LEVEL_1);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, loa1PredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingAllLevelsIdp,
                onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForSignInForNonOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> signInPredicateNonOnboarding = idpPredicateFactory
                .createPredicateForSignIn(transactionEntityNonOnboarding);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, signInPredicateNonOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp,
                nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing,
                nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }

    @Test
    public void shouldReturnIdpsForSignInForOnboardingTransactionEntity() {
        final Predicate<IdentityProviderConfig> signInPredicateOnboarding = idpPredicateFactory
                .createPredicateForSignIn(transactionEntityOnboarding);

        final Set<IdentityProviderConfig> filteredIdps = getFilteredIdps(allIdps, signInPredicateOnboarding);

        final IdentityProviderConfig[] expectedFilteredIdps = {nonOnboardingLoa1Idp, nonOnboardingLoa2Idp,
                nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp,
                onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity,
                onboardingSoftDisconnectingIdp, onboardingHardDisconnectingIdp,
                nonOnboardingSoftDisconnectingIdp, nonOnboardingSoftDisconnectingIdpEnabledForIdpResponseProcessing,
                nonOnboardingHardDisconnectingIdp};

        assertThat(filteredIdps).containsOnly(expectedFilteredIdps);
    }
}
