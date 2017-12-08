package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.CountriesConfigEntityData;

public class CountriesConfigEntityDataBuilder {

    private String entityId = "default-country-entity-id";
    private String simpleId = "default-country-simple-id";
    private boolean enabled = true;
    private String overriddenSsoUrl = "http://default.country/SSO";


    public static CountriesConfigEntityDataBuilder aCountriesConfigEntityData() {
        return new CountriesConfigEntityDataBuilder();
    }

    public CountriesConfigEntityData build() {
        return new TestCountriesConfigEntityData(
            entityId,
            simpleId,
            enabled,
            overriddenSsoUrl
        );
    }

    public CountriesConfigEntityDataBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public CountriesConfigEntityDataBuilder withSimpleId(String simpleId) {
        this.simpleId = simpleId;
        return this;
    }

    public CountriesConfigEntityDataBuilder withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public CountriesConfigEntityDataBuilder withOverriddenSsoUrl(String overriddenSsoUrl) {
        this.overriddenSsoUrl = overriddenSsoUrl;
        return this;
    }

    private static class TestCountriesConfigEntityData extends CountriesConfigEntityData {

        private TestCountriesConfigEntityData(
                String entityId,
                String simpleId,
                boolean enabled,
                String overriddenSsoUrl) {

            this.entityId = entityId;
            this.simpleId = simpleId;
            this.enabled = enabled;
            this.overriddenSsoUrl = overriddenSsoUrl;
        }
    }
}
