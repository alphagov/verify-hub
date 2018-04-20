package uk.gov.ida.saml.msa.test.domain;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Attribute;
import uk.gov.ida.saml.core.domain.AssertionRestrictions;
import uk.gov.ida.saml.core.domain.MatchingServiceAuthnStatement;
import uk.gov.ida.saml.core.domain.OutboundAssertion;
import uk.gov.ida.saml.core.domain.PersistentId;

import java.util.List;

public class MatchingServiceAssertion extends OutboundAssertion {
    private MatchingServiceAuthnStatement authnStatement;
    private String audience;
    private List<Attribute> userAttributesForAccountCreation;

    public MatchingServiceAssertion(
            String id,
            String issuerId,
            DateTime issueInstant,
            PersistentId persistentId,
            AssertionRestrictions assertionRestrictions,
            MatchingServiceAuthnStatement authnStatement,
            String audience,
            List<Attribute> userAttributesForAccountCreation) {

        super(id, issuerId, issueInstant, persistentId, assertionRestrictions);

        this.authnStatement = authnStatement;
        this.audience = audience;
        this.userAttributesForAccountCreation = userAttributesForAccountCreation;
    }

    public MatchingServiceAuthnStatement getAuthnStatement(){
        return authnStatement;
    }

    public String getAudience() {
        return audience;
    }

    public List<Attribute> getUserAttributesForAccountCreation() {
        return userAttributesForAccountCreation;
    }
}
