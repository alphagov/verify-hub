package uk.gov.ida.saml.msa.test.outbound.transformers;

import com.google.inject.Inject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.impl.AttributeStatementBuilder;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.MatchingServiceAuthnStatement;
import uk.gov.ida.saml.core.transformers.outbound.OutboundAssertionToSubjectTransformer;
import uk.gov.ida.saml.msa.test.domain.MatchingServiceAssertion;

import java.util.List;

public class MatchingServiceAssertionToAssertionTransformer {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;
    private final MatchingServiceAuthnStatementToAuthnStatementTransformer matchingServiceAuthnStatementToAuthnStatementTransformer;
    private final OutboundAssertionToSubjectTransformer outboundAssertionToSubjectTransformer;
    private XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();

    @Inject
    public MatchingServiceAssertionToAssertionTransformer(
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            MatchingServiceAuthnStatementToAuthnStatementTransformer matchingServiceAuthnStatementToAuthnStatementTransformer,
            OutboundAssertionToSubjectTransformer outboundAssertionToSubjectTransformer) {

        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
        this.matchingServiceAuthnStatementToAuthnStatementTransformer = matchingServiceAuthnStatementToAuthnStatementTransformer;
        this.outboundAssertionToSubjectTransformer = outboundAssertionToSubjectTransformer;
    }

    public Assertion transform(MatchingServiceAssertion originalAssertion) {

        Assertion transformedAssertion = openSamlXmlObjectFactory.createAssertion();
        transformedAssertion.setIssueInstant(originalAssertion.getIssueInstant());

        Issuer transformedIssuer = openSamlXmlObjectFactory.createIssuer(originalAssertion.getIssuerId());
        transformedAssertion.setIssuer(transformedIssuer);
        transformedAssertion.setID(originalAssertion.getId());

        Subject subject = outboundAssertionToSubjectTransformer.transform(originalAssertion);
        transformedAssertion.setSubject(subject);

        MatchingServiceAuthnStatement authnStatement = originalAssertion.getAuthnStatement();

        transformedAssertion.getAuthnStatements().add(matchingServiceAuthnStatementToAuthnStatementTransformer.transform(authnStatement));

        Conditions conditions = openSamlXmlObjectFactory.createConditions();
        AudienceRestriction audienceRestriction = openSamlXmlObjectFactory.createAudienceRestriction(originalAssertion.getAudience());
        conditions.getAudienceRestrictions().add(audienceRestriction);
        transformedAssertion.setConditions(conditions);

        List<Attribute> userAttributesForAccountCreation = originalAssertion.getUserAttributesForAccountCreation();
        if (!userAttributesForAccountCreation.isEmpty()) {
            addAttributes(transformedAssertion, userAttributesForAccountCreation);
        }


        return transformedAssertion;
    }

    private void addAttributes(final Assertion transformedAssertion, final List<Attribute> userAttributesForAccountCreation) {
        AttributeStatementBuilder attributeStatementBuilder =
                (AttributeStatementBuilder) builderFactory.getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
        AttributeStatement attributeStatement = attributeStatementBuilder.buildObject();
        attributeStatement.getAttributes().addAll(userAttributesForAccountCreation);

        transformedAssertion.getAttributeStatements().add(attributeStatement);
    }

}
