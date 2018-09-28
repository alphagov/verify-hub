package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.ListUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import uk.gov.ida.hub.config.ConfigEntityData;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProviderConfigEntityData implements ConfigEntityData {

    @SuppressWarnings("unused") // needed to prevent guice injection
    protected IdentityProviderConfigEntityData() {
    }

    // If present, the IDP will only be visible when fulfilling a request from
    // the specified onboardingTransactionEntity.
    // If absent, this IDP will be available to all
    @Valid
    @JsonProperty
    protected List<String> onboardingTransactionEntityIds = ImmutableList.of();

    // This is a temporary field to be used for zero down time while we change how we configure onboarding idps
    @Valid
    @JsonProperty
    protected List<String> onboardingTransactionEntityIdsTemp = ImmutableList.of();

    @Valid
    @NotNull
    @JsonProperty
    protected String entityId;

    @Valid
    @NotNull
    @JsonProperty
    protected String simpleId;

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean enabled;

    @Valid
    @JsonProperty
    protected Boolean enabledForSingleIdp = false;

    @Valid
    @JsonProperty
    protected String provideRegistrationUntil;
    
    @Valid
    @JsonProperty
    protected String provideAuthenticationUntil;
    
    @Valid
    @NotNull
    @JsonProperty
    protected List<LevelOfAssurance> supportedLevelsOfAssurance;

    @Valid
    @NotNull
    @JsonProperty
    protected List<LevelOfAssurance> onboardingLevelsOfAssurance = ImmutableList.of();

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean useExactComparisonType;

    @Override
    public String getEntityId() {
        return entityId;
    }

    @Override
    public String toString() {
        return String.format("%s | %s", this.getEntityId(), this.getSimpleId());
    }

    public List<LevelOfAssurance> getSupportedLevelsOfAssurance() {
        return supportedLevelsOfAssurance;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public Boolean isEnabled() {
        return enabled;
    }
    
    public Boolean isRegistrationEnabled() {
        if (Strings.isNullOrEmpty(provideRegistrationUntil)) {
            return true;
        }
        
        DateTime provideRegistrationUntilDate = DateTime.parse(provideRegistrationUntil, DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ"));
        
        return provideRegistrationUntilDate.isAfterNow();
    }
    
    @JsonProperty("authenticationEnabled")
    public Boolean isAuthenticationEnabled() {
        if (Strings.isNullOrEmpty(provideAuthenticationUntil)) {
            return true;
        }

        DateTime provideAuthenticationUntilDate = DateTime.parse(provideAuthenticationUntil, DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ"));
        
        return provideAuthenticationUntilDate.isAfterNow();
    }

    public Boolean isEnabledForSingleIdp() {
        return enabledForSingleIdp;
    }

    public List<String> getOnboardingTransactionEntityIds() {
        return onboardingTransactionEntityIds;
    }

    public List<String> getOnboardingTransactionEntityIdsTemp() {
        return ListUtils.union(onboardingTransactionEntityIdsTemp, onboardingTransactionEntityIds);
    }

    public boolean isOnboardingForTransactionEntity(String transactionEntity) {
        return this.getOnboardingTransactionEntityIdsTemp().contains(transactionEntity);
    }

    public boolean isOnboardingAtAllLevels() {
        return supportedLevelsOfAssurance.stream().allMatch(this::isOnboardingAtLoa);
    }

    public boolean isOnboardingAtLoa(LevelOfAssurance levelOfAssurance) {
        return onboardingLevelsOfAssurance.contains(levelOfAssurance);
    }

    public Boolean getUseExactComparisonType() {
        return useExactComparisonType;
    }
}
