package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.net.URI;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EidasCountryDto {

    private String entityId;
    private String simpleId;
    private boolean enabled;
    private URI overriddenSsoUrl;


    @SuppressWarnings("unused") // NEEDED BY JAXB
    protected EidasCountryDto() {
    }

    public EidasCountryDto(
        final String entityId,
        final String simpleId,
        final boolean enabled) {
        this.entityId = entityId;
        this.simpleId = simpleId;
        this.enabled = enabled;
        this.overriddenSsoUrl = null;
    }

    public EidasCountryDto(
            final String entityId,
            final String simpleId,
            final boolean enabled,
            final URI overriddenSsoUrl) {
        this(entityId, simpleId, enabled);
        this.overriddenSsoUrl = overriddenSsoUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        EidasCountryDto that = (EidasCountryDto) obj;

        return Objects.equals(this.getEntityId(), that.getEntityId())
            && Objects.equals(this.getSimpleId(), that.getSimpleId())
            && Objects.equals(this.getOverriddenSsoUrl(), that.getOverriddenSsoUrl())
            && this.getEnabled() == that.getEnabled();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntityId(), getSimpleId(), getEnabled(), getOverriddenSsoUrl());
    }

    public String getEntityId() {
        return entityId;
    }

    public String getSimpleId() {
        return simpleId;
    }

    public URI getOverriddenSsoUrl() {
        return overriddenSsoUrl;
    }

    public boolean getEnabled() {
        return enabled;
    }
}
