package uk.gov.ida.saml.core.test.builders;

import org.opensaml.saml.saml2.core.AudienceRestriction;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.test.TestEntityIds;

public class AudienceRestrictionBuilder {

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
    private String audienceId = TestEntityIds.HUB_ENTITY_ID;

    public static AudienceRestrictionBuilder anAudienceRestriction() {
        return new AudienceRestrictionBuilder();
    }

    public AudienceRestriction build() {
        return openSamlXmlObjectFactory.createAudienceRestriction(audienceId);
    }

    public AudienceRestrictionBuilder withAudienceId(String audienceId) {
        this.audienceId = audienceId;
        return this;
    }
}
