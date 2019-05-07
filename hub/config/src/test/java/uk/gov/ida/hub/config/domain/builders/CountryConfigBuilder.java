package uk.gov.ida.hub.config.domain.builders;

import uk.gov.ida.hub.config.domain.CountryConfig;

public class CountryConfigBuilder {

    private String entityId = "default-country-entity-id";
    private String simpleId = "default-country-simple-id";
    private boolean enabled = true;
    private String overriddenSsoUrl = "http://default.country/SSO";


    public static CountryConfigBuilder aCountryConfig() {
        return new CountryConfigBuilder();
    }

    public CountryConfig build() {
        return new TestCountryConfig(
            entityId,
            simpleId,
            enabled,
            overriddenSsoUrl
        );
    }

    public CountryConfigBuilder withEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }

    public CountryConfigBuilder withSimpleId(String simpleId) {
        this.simpleId = simpleId;
        return this;
    }

    public CountryConfigBuilder withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public CountryConfigBuilder withOverriddenSsoUrl(String overriddenSsoUrl) {
        this.overriddenSsoUrl = overriddenSsoUrl;
        return this;
    }

    private static class TestCountryConfig extends CountryConfig {

        private TestCountryConfig(
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
