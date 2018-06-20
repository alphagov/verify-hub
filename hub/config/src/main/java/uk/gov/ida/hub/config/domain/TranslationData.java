package uk.gov.ida.hub.config.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.config.ConfigEntityData;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

public class TranslationData implements ConfigEntityData {
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
        @NotNull
        @JsonProperty
        protected String taxonName;
    }

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
    public String getEntityId() {
        return getSimpleId();
    }

    public Optional<Translation> getTranslationsByLocale(String locale) {
        return translations
                .stream()
                .filter(translation -> locale.equals(translation.locale))
                .findFirst();
    }
}
