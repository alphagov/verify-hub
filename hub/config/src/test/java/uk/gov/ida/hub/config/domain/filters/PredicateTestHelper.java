package uk.gov.ida.hub.config.domain.filters;

import org.joda.time.DateTime;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;

import java.util.function.Predicate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

final class PredicateTestHelper {

    private PredicateTestHelper() {
    }

    private static final IdentityProviderConfigDataBuilder builder = anIdentityProviderConfigData();

    static final String transactionEntityNonOnboarding = "transactionEntityNonOnboarding";
    static final String transactionEntityOnboarding = "transactionEntityOnboarding";
    static final String transactionEntityOnboardingOther = "transactionEntityOnboardingOther";

    static final DateTime expiredDatetime = DateTime.now().minusDays(1);
    static final DateTime futureDatetime = DateTime.now().plusDays(1);

    static final IdentityProviderConfig nonOnboardingLoa1Idp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_1))
            .withoutOnboarding()
            .withSimpleId("nonOnboardingLoa1Idp")
            .build();

    static final IdentityProviderConfig nonOnboardingLoa2Idp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
            .withoutOnboarding()
            .withSimpleId("nonOnboardingLoa2Idp")
            .build();

    static final IdentityProviderConfig nonOnboardingAllLevelsIdp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withoutOnboarding()
            .withSimpleId("nonOnboardingAllLevelsIdp")
            .build();

    static final IdentityProviderConfig nonOnboardingSoftDisconnectingIdp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withoutOnboarding()
            .withProvideRegistrationUntil(expiredDatetime)
            .withProvideAuthenticationUntil(futureDatetime)
            .withSimpleId("nonOnboardingSoftDisconnectingIdp")
            .build();

    static final IdentityProviderConfig nonOnboardingHardDisconnectingIdp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withoutOnboarding()
            .withProvideRegistrationUntil(expiredDatetime)
            .withProvideAuthenticationUntil(expiredDatetime)
            .withSimpleId("nonOnboardingHardDisconnectingIdp")
            .build();

    static final IdentityProviderConfig onboardingLoa1Idp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withSimpleId("onboardingLoa1Idp")
            .build();

    static final IdentityProviderConfig onboardingSoftDisconnectingIdp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withProvideRegistrationUntil(expiredDatetime)
            .withProvideAuthenticationUntil(futureDatetime)
            .withSimpleId("onboardingSoftDisconnectingIdp")
            .build();

    static final IdentityProviderConfig onboardingHardDisconnectingIdp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withProvideRegistrationUntil(expiredDatetime)
            .withProvideAuthenticationUntil(expiredDatetime)
            .withSimpleId("onboardingHardDisconnectingIdp")
            .build();

    static final IdentityProviderConfig onboardingLoa1IdpOtherOnboardingEntity = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
            .withOnboarding(Collections.singletonList(transactionEntityOnboardingOther))
            .withSimpleId("onboardingLoa1IdpOtherOnboardingEntity")
            .build();

    static final IdentityProviderConfig onboardingLoa2Idp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withSimpleId("onboardingLoa2Idp")
            .build();

    static final IdentityProviderConfig onboardingLoa2IdpOtherOnboardingEntity = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboardingOther))
            .withSimpleId("onboardingLoa2IdpOtherOnboardingEntity")
            .build();

    static final IdentityProviderConfig onboardingAllLevelsIdp = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withSimpleId("onboardingAllLevelsIdp")
            .build();

    static final IdentityProviderConfig onboardingAllLevelsIdpOtherOnboardingEntity = anIdentityProviderConfigData()
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboardingOther))
            .withSimpleId("onboardingAllLevelsIdpOtherOnboardingEntity")
            .build();

    static final Set<IdentityProviderConfig> allIdps = new HashSet<>(Arrays.asList(nonOnboardingLoa1Idp,
            nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp, nonOnboardingSoftDisconnectingIdp, nonOnboardingHardDisconnectingIdp,
            onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp, onboardingLoa1IdpOtherOnboardingEntity,
            onboardingLoa2IdpOtherOnboardingEntity, onboardingAllLevelsIdpOtherOnboardingEntity, onboardingSoftDisconnectingIdp,
            onboardingHardDisconnectingIdp));

    static Set<IdentityProviderConfig> getFilteredIdps(Set<IdentityProviderConfig> idpSet,
                                                       Set<Predicate<IdentityProviderConfig>> predicateSet) {
        return idpSet.stream()
                .filter(predicateSet.stream().reduce(Predicate::and).orElseThrow())
                .collect(Collectors.toSet());
    }

    static Set<IdentityProviderConfig> getFilteredIdps(Set<IdentityProviderConfig> idpSet,
                                                       Predicate<IdentityProviderConfig> predicate) {
        return idpSet.stream().filter(predicate).collect(Collectors.toSet());
    }
}
