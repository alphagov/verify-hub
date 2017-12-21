package uk.gov.ida.hub.config.domain.filters;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static uk.gov.ida.hub.config.domain.builders.IdentityProviderConfigDataBuilder.anIdentityProviderConfigData;

final class PredicateTestHelper {

    private PredicateTestHelper() { }

    private static final IdentityProviderConfigDataBuilder builder = anIdentityProviderConfigData();

    static final String transactionEntityNonOnboarding = "transactionEntityNonOnboarding";
    static final String transactionEntityOnboarding = "transactionEntityOnboarding";
    static final String transactionEntityOnboardingOther = "transactionEntityOnboardingOther";

    static final IdentityProviderConfigEntityData nonOnboardingLoa1Idp = builder
            .withSupportedLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_1))
            .withoutOnboarding()
            .withSimpleId("nonOnboardingLoa1Idp")
            .build();

    static final IdentityProviderConfigEntityData nonOnboardingLoa2Idp = builder
            .withSupportedLevelsOfAssurance(Collections.singletonList(LevelOfAssurance.LEVEL_2))
            .withoutOnboarding()
            .withSimpleId("nonOnboardingLoa2Idp")
            .build();

    static final IdentityProviderConfigEntityData nonOnboardingAllLevelsIdp = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withoutOnboarding()
            .withSimpleId("nonOnboardingAllLevelsIdp")
            .build();

    static final IdentityProviderConfigEntityData onboardingLoa1Idp = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withSimpleId("onboardingLoa1Idp")
            .build();

    static final IdentityProviderConfigEntityData onboardingLoa1IdpOtherOnboardingEntity = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_1))
            .withOnboarding(Collections.singletonList(transactionEntityOnboardingOther))
            .withSimpleId("onboardingLoa1IdpOtherOnboardingEntity")
            .build();

    static final IdentityProviderConfigEntityData onboardingLoa2Idp = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withSimpleId("onboardingLoa2Idp")
            .build();

    static final IdentityProviderConfigEntityData onboardingLoa2IdpOtherOnboardingEntity = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Collections.singletonList(LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboardingOther))
            .withSimpleId("onboardingLoa2IdpOtherOnboardingEntity")
            .build();

    static final IdentityProviderConfigEntityData onboardingAllLevelsIdp = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboarding))
            .withSimpleId("onboardingAllLevelsIdp")
            .build();

    static final IdentityProviderConfigEntityData onboardingAllLevelsIdpOtherOnboardingEntity = builder
            .withSupportedLevelsOfAssurance(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboardingLevels(Arrays.asList(LevelOfAssurance.LEVEL_1, LevelOfAssurance.LEVEL_2))
            .withOnboarding(Collections.singletonList(transactionEntityOnboardingOther))
            .withSimpleId("onboardingAllLevelsIdpOtherOnboardingEntity")
            .build();

    static final Set<IdentityProviderConfigEntityData> allIdps = new HashSet<>(Arrays.asList(nonOnboardingLoa1Idp,
            nonOnboardingLoa2Idp, nonOnboardingAllLevelsIdp, onboardingLoa1Idp, onboardingLoa2Idp, onboardingAllLevelsIdp,
            onboardingLoa1IdpOtherOnboardingEntity, onboardingLoa2IdpOtherOnboardingEntity, onboardingAllLevelsIdpOtherOnboardingEntity));

    static Set<IdentityProviderConfigEntityData> getFilteredIdps(Set<IdentityProviderConfigEntityData> idpSet,
                                                                 Set<Predicate<IdentityProviderConfigEntityData>> predicateSet) {
        return Sets.filter(idpSet, Predicates.and(predicateSet));
    }

    static Set<IdentityProviderConfigEntityData> getFilteredIdps(Set<IdentityProviderConfigEntityData> idpSet,
                                                                 Predicate<IdentityProviderConfigEntityData> predicate) {
        return Sets.filter(idpSet, predicate);
    }
}
