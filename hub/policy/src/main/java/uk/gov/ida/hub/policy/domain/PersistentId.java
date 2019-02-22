package uk.gov.ida.hub.policy.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

@Immutable
public final class PersistentId implements Serializable {

    @JsonProperty
    private String nameId;

    @SuppressWarnings("unused")//Needed by JAXB
    private PersistentId() { }

    @JsonCreator
    public PersistentId(@JsonProperty("nameId") String nameId) {
        this.nameId = nameId;
    }

    public String getNameId() {
        return nameId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PersistentId{");
        sb.append("nameId='").append(nameId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PersistentId that = (PersistentId) o;

        return new EqualsBuilder()
            .append(nameId, that.nameId)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(nameId)
            .toHashCode();
    }
}
