package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.config.ConfigEntityData;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

public class TranslationData implements ConfigEntityData {
    @Valid
    @NotNull
    @JsonProperty
    protected String simpleId;

    @Valid
    @NotNull
    @JsonProperty
    protected List<Translation> translations;

    public String getSimpleId() {
        return simpleId;
    }

    @Override
    @JsonIgnore
    public String getEntityId() {
        // This just returns the simple ID, as translations don't have entity IDs and `getEntityId()`
        // must return an identifier (which is used by other parts of the application as cache key).
        return getSimpleId();
    }

    public Optional<Translation> getTranslationsByLocale(String locale) {
        return translations
                .stream()
                .filter(translation -> locale.equals(translation.locale))
                .findFirst();
    }

    public static class Translation {
        @Valid
        @NotNull
        @JsonProperty
        protected String locale;

        @Valid
        @NotNull
        @JsonProperty
        protected String name;

        @Valid
        @NotNull
        @JsonProperty
        protected String rpName;

        @Valid
        @NotNull
        @JsonProperty
        protected String analyticsDescription;

        @Valid
        @NotNull
        @JsonProperty
        protected String otherWaysDescription;

        @Valid
        @NotNull
        @JsonProperty
        protected String otherWaysText;

        @Valid
        @NotNull
        @JsonProperty
        protected String tailoredText;

        @Valid
        @JsonProperty
        protected String taxonName;

        @Valid
        @JsonProperty
        protected String customFailHeading;

        @Valid
        @JsonProperty
        protected String customFailWhatNextContent;

        @Valid
        @JsonProperty
        protected String customFailOtherOptions;

        @Valid
        @JsonProperty
        protected String customFailTryAnotherSummary;

        @Valid
        @JsonProperty
        protected String customFailTryAnotherText;

        @Valid
        @JsonProperty
        protected String customFailContactDetailsIntro;

        public Translation setLocale(String locale) {
            this.locale = locale;
            return this;
        }

        public Translation setName(String name) {
            this.name = name;
            return this;
        }

        public Translation setAnalyticsDescription(String analyticsDescription) {
            this.analyticsDescription = analyticsDescription;
            return this;
        }

        public Translation setOtherWaysDescription(String otherWaysDescription) {
            this.otherWaysDescription = otherWaysDescription;
            return this;
        }

        public Translation setOtherWaysText(String otherWaysText) {
            this.otherWaysText = otherWaysText;
            return this;
        }

        public Translation setRpName(String rpName) {
            this.rpName = rpName;
            return this;
        }

        public Translation setTailoredText(String tailoredText) {
            this.tailoredText = tailoredText;
            return this;
        }

        public Translation setTaxonName(String taxonName) {
            this.taxonName = taxonName;
            return this;
        }
    }
}
