package uk.gov.ida.hub.samlengine.logging.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.ida.hub.samlengine.domain.LevelOfAssurance;

import java.util.List;
import java.util.Map;

public class AttributeStatementLogData {

    private final String issuer;
    private final LevelOfAssurance levelOfAssurance;
    private final Map<String, List<VerifiedAttributeLogData>> attributes;

    @JsonCreator
    public AttributeStatementLogData(
        @JsonProperty("issuer") String issuer,
        @JsonProperty("levelOfAssurance") LevelOfAssurance levelOfAssurance,
        @JsonProperty("attributes") Map<String, List<VerifiedAttributeLogData>> attributes
    ) {
        this.issuer = issuer;
        this.levelOfAssurance = levelOfAssurance;
        this.attributes = attributes;
    }

    public String getIssuer() {
        return issuer;
    }

    public LevelOfAssurance getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public Map<String, List<VerifiedAttributeLogData>> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeStatementLogData that = (AttributeStatementLogData) o;

        if (issuer != null ? !issuer.equals(that.issuer) : that.issuer != null) return false;
        if (levelOfAssurance != that.levelOfAssurance) return false;
        return attributes != null ? attributes.equals(that.attributes) : that.attributes == null;
    }

    @Override
    public String toString() {
        return "AttributeStatementLogDto{" +
            "issuer='" + issuer + '\'' +
            ", levelOfAssurance=" + levelOfAssurance +
            ", attributes=" + attributes +
            '}';
    }

    @Override
    public int hashCode() {
        int result = issuer != null ? issuer.hashCode() : 0;
        result = 31 * result + (levelOfAssurance != null ? levelOfAssurance.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }
}
