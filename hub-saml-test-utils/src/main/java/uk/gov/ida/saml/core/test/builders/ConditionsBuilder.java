package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.Conditions;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.ida.saml.core.test.builders.AudienceRestrictionBuilder.anAudienceRestriction;

public class ConditionsBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private List<AudienceRestriction> audienceRestrictions = new ArrayList<>();
    private AudienceRestriction defaultAudienceRestriction = anAudienceRestriction().build();
    private boolean shouldIncludeDefaultAudienceRestriction = true;

    public static ConditionsBuilder aConditions() {
        return new ConditionsBuilder();
    }

    public Conditions build() {
        Conditions conditions = openSamlXmlObjectFactory.createConditions();

        if (shouldIncludeDefaultAudienceRestriction) {
            audienceRestrictions.add(defaultAudienceRestriction);
        }
        conditions.getAudienceRestrictions().addAll(audienceRestrictions);

        return conditions;
    }

    public ConditionsBuilder withoutDefaultAudienceRestriction() {
        shouldIncludeDefaultAudienceRestriction = false;
        return this;
    }

    public ConditionsBuilder addAudienceRestriction(AudienceRestriction audienceRestriction) {
        shouldIncludeDefaultAudienceRestriction = false;
        audienceRestrictions.add(audienceRestriction);
        return this;
    }
}
