package uk.gov.ida.hub.config.domain.builders;


import org.joda.time.DateTime;
import uk.gov.ida.hub.config.domain.IdentityProviderConfig;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;

import java.util.List;

import static java.util.Collections.emptyList;


public class IdentityProviderConfigDataBuilder {

    private String entityId = "default-idp-entity-id";
    private String simpleId = "default-idp-simple-id";
    private Boolean enabled = true;
    private Boolean enabledForSingleIdp = false;
    private List<String> transactionEntityIds = emptyList();
    private List<LevelOfAssurance> onboardingLevelsOfAssurance = emptyList();
    private List<LevelOfAssurance> supportedLevelsOfAssurance = List.of(LevelOfAssurance.LEVEL_2);
    private boolean useExactComparisonType = false;
    private String provideRegistrationUntil;
    private String provideAuthenticationUntil;

    public static IdentityProviderConfigDataBuilder anIdentityProviderConfigData() {
        return new IdentityProviderConfigDataBuilder();
    }

    public IdentityProviderConfig build() {
        return new TestIdentityProviderConfig(
                entityId,
                simpleId,
                enabled,
                enabledForSingleIdp,
                transactionEntityIds,
                onboardingLevelsOfAssurance,
                supportedLevelsOfAssurance,
                useExactComparisonType,
                provideRegistrationUntil,
                provideAuthenticationUntil
        );
    }

    public IdentityProviderConfigDataBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public IdentityProviderConfigDataBuilder withSimpleId(String simpleId) {
        this.simpleId = simpleId;
        return this;
    }

    public IdentityProviderConfigDataBuilder withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    
    public IdentityProviderConfigDataBuilder withProvideRegistrationUntil(DateTime provideRegistrationUntil) {
        this.provideRegistrationUntil = provideRegistrationUntil.toString("yyyy-MM-dd'T'HH:mm:ssZZ");
        return this;
    }
    
    public IdentityProviderConfigDataBuilder withProvideAuthenticationUntil(DateTime provideAuthenticationUntil) {
        this.provideAuthenticationUntil = provideAuthenticationUntil.toString("yyyy-MM-dd'T'HH:mm:ssZZ");
        return this;
    }

    public IdentityProviderConfigDataBuilder withOnboarding(List<String> transactionEntityIds) {
        this.transactionEntityIds = transactionEntityIds;
        return this;
    }

    public IdentityProviderConfigDataBuilder withOnboardingLevels(List<LevelOfAssurance> onboardingLevelsOfAssurance) {
        this.onboardingLevelsOfAssurance = onboardingLevelsOfAssurance;
        return this;
    }

    public IdentityProviderConfigDataBuilder withoutOnboarding() {
        this.transactionEntityIds = emptyList();
        this.onboardingLevelsOfAssurance = emptyList();
        return this;
    }

    public IdentityProviderConfigDataBuilder withSupportedLevelsOfAssurance(List<LevelOfAssurance> levelsOfAssurance) {
        this.supportedLevelsOfAssurance = levelsOfAssurance;
        return this;
    }

    public IdentityProviderConfigDataBuilder withEnabledForSingleIdp(boolean enabledForSingleIdp) {
        this.enabledForSingleIdp = enabledForSingleIdp;
        return this;
    }

    private static class TestIdentityProviderConfig extends IdentityProviderConfig {

        private TestIdentityProviderConfig(
                String entityId,
                String simpleId,
                boolean enabled,
                boolean enabledForSingleIdp,
                List<String> transactionEntityIds,
                List<LevelOfAssurance> onboardingLevelsOfAssurance,
                List<LevelOfAssurance> supportedLevelsOfAssurance,
                boolean useExactComparisonType,
                String provideRegistrationUntil,
                String provideAuthenticationUntil) {

            this.entityId = entityId;
            this.simpleId = simpleId;
            this.enabled = enabled;
            this.enabledForSingleIdp = enabledForSingleIdp;
            this.onboardingTransactionEntityIds = transactionEntityIds;
            this.onboardingLevelsOfAssurance = onboardingLevelsOfAssurance;
            this.supportedLevelsOfAssurance = supportedLevelsOfAssurance;
            this.useExactComparisonType = useExactComparisonType;
            this.provideRegistrationUntil = provideRegistrationUntil;
            this.provideAuthenticationUntil = provideAuthenticationUntil;
        }
    }
}
