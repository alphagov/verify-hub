package uk.gov.ida.hub.config.domain.builders;


import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import uk.gov.ida.hub.config.domain.IdentityProviderConfigEntityData;
import uk.gov.ida.hub.config.domain.LevelOfAssurance;
import uk.gov.ida.hub.config.domain.SignatureVerificationCertificate;

import java.util.ArrayList;
import java.util.List;


public class IdentityProviderConfigDataBuilder {

    private String entityId = "default-idp-entity-id";
    private List<SignatureVerificationCertificate> signatureVerificationCertificates = new ArrayList<>();
    private String simpleId = "default-idp-simple-id";
    private Boolean enabled = true;
    private Boolean enabledForSingleIdp = false;
    private List<String> transactionEntityIds = ImmutableList.of();
    private List<String> transactionEntityIdsTemp = ImmutableList.of();
    private List<LevelOfAssurance> onboardingLevelsOfAssurance = ImmutableList.of();
    private List<LevelOfAssurance> supportedLevelsOfAssurance = ImmutableList.of(LevelOfAssurance.LEVEL_2);
    private boolean useExactComparisonType = false;
    private String provideRegistrationUntil;
    private String provideAuthenticationUntil;

    public static IdentityProviderConfigDataBuilder anIdentityProviderConfigData() {
        return new IdentityProviderConfigDataBuilder();
    }

    public IdentityProviderConfigEntityData build() {
        if (signatureVerificationCertificates.isEmpty()) {
            signatureVerificationCertificates.add(new SignatureVerificationCertificateBuilder().build());
        }

        return new TestIdentityProviderConfigEntityData(
                entityId,
                simpleId,
                enabled,
                enabledForSingleIdp,
                transactionEntityIds,
                transactionEntityIdsTemp,
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

    public IdentityProviderConfigDataBuilder addSignatureVerificationCertificate(SignatureVerificationCertificate certificate) {
        this.signatureVerificationCertificates.add(certificate);
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

    public IdentityProviderConfigDataBuilder withOnboardingTemp(List<String> transactionEntityIdsTemp) {
        this.transactionEntityIdsTemp = transactionEntityIdsTemp;
        return this;
    }

    public IdentityProviderConfigDataBuilder withOnboardingLevels(List<LevelOfAssurance> onboardingLevelsOfAssurance) {
        this.onboardingLevelsOfAssurance = onboardingLevelsOfAssurance;
        return this;
    }

    public IdentityProviderConfigDataBuilder withoutOnboarding() {
        this.transactionEntityIds = ImmutableList.of();
        this.onboardingLevelsOfAssurance = ImmutableList.of();
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

    private static class TestIdentityProviderConfigEntityData extends IdentityProviderConfigEntityData {

        private TestIdentityProviderConfigEntityData(
                String entityId,
                String simpleId,
                boolean enabled,
                boolean enabledForSingleIdp,
                List<String> transactionEntityIds,
                List<String> transactionEntityIdsTemp,
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
            this.onboardingTransactionEntityIdsTemp = transactionEntityIdsTemp;
            this.supportedLevelsOfAssurance = supportedLevelsOfAssurance;
            this.useExactComparisonType = useExactComparisonType;
            this.provideRegistrationUntil = provideRegistrationUntil;
            this.provideAuthenticationUntil = provideAuthenticationUntil;
        }
    }
}
