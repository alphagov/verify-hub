package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

import static java.util.Collections.emptyList;


@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProviderConfig implements EntityIdentifiable {

    @Valid
    @JsonProperty
    protected List<String> onboardingTransactionEntityIds = emptyList();

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
    protected List<LevelOfAssurance> onboardingLevelsOfAssurance = emptyList();

    @Valid
    @NotNull
    @JsonProperty
    protected Boolean useExactComparisonType;

    @Valid
    @JsonProperty
    protected Boolean temporarilyUnavailable = false;

    @SuppressWarnings("unused") // needed to prevent guice injection
    protected IdentityProviderConfig() {
    }

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

    public boolean supportsLoa(LevelOfAssurance levelOfAssurance) {
        return supportedLevelsOfAssurance.contains(levelOfAssurance);
    }

    public String getSimpleId() {
        return simpleId;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    @JsonProperty("temporarilyUnavailable")
    public Boolean isTemporarilyUnavailable() {
        return temporarilyUnavailable;
    }

    public Boolean canReceiveRegistrationRequests() {
        return Strings.isNullOrEmpty(provideRegistrationUntil) || provideRegistrationUntilDate().isAfterNow();
    }

    public Boolean canSendRegistrationResponses(Duration sessionDuration) {
        return Strings.isNullOrEmpty(provideRegistrationUntil) || provideRegistrationUntilDate().withDurationAdded(sessionDuration, 1).isAfterNow();
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

    public String getProvideRegistrationUntil() {
        return provideRegistrationUntil;
    }

    public String getProvideAuthenticationUntil() {
        return provideAuthenticationUntil;
    }

    public List<String> getOnboardingTransactionEntityIds() {
        return onboardingTransactionEntityIds;
    }

    public boolean isOnboardingForTransactionEntityAtLoa(String transactionEntity, LevelOfAssurance levelOfAssurance) {
        boolean isOnboarding = levelOfAssurance != null ?
                isOnboardingAtLoa(levelOfAssurance) :
                isOnboardingAtAllLevels();

        return !isOnboarding || getOnboardingTransactionEntityIds().contains(transactionEntity);
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

    private DateTime provideRegistrationUntilDate() {
        return DateTime.parse(provideRegistrationUntil, DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ"));
    }
}
