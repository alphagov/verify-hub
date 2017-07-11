package uk.gov.ida.saml.hub.transformers.outbound;

import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Issuer;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.Cycle3Dataset;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.transformers.outbound.OutboundAssertionToSubjectTransformer;
import uk.gov.ida.saml.hub.factories.AttributeFactory;

import java.util.Map;

public class HubAssertionMarshaller {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;
    private final AttributeFactory attributeFactory;
    private final OutboundAssertionToSubjectTransformer outboundAssertionToSubjectTransformer;

    public HubAssertionMarshaller(
            OpenSamlXmlObjectFactory openSamlXmlObjectFactory,
            AttributeFactory attributeFactory,
            OutboundAssertionToSubjectTransformer outboundAssertionToSubjectTransformer) {

        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
        this.attributeFactory = attributeFactory;
        this.outboundAssertionToSubjectTransformer = outboundAssertionToSubjectTransformer;
    }

    public Assertion toSaml(HubAssertion hubAssertion) {

        Assertion transformedAssertion = openSamlXmlObjectFactory.createAssertion();
        transformedAssertion.setIssueInstant(hubAssertion.getIssueInstant());
        Issuer transformedIssuer = openSamlXmlObjectFactory.createIssuer(hubAssertion.getIssuerId());
        transformedAssertion.setIssuer(transformedIssuer);
        transformedAssertion.setID(hubAssertion.getId());

        if (hubAssertion.getCycle3Data().isPresent()){
            Cycle3Dataset cycle3Data = hubAssertion.getCycle3Data().get();
            transformedAssertion.getAttributeStatements().add(transform(cycle3Data));
        }

        transformedAssertion.setSubject(outboundAssertionToSubjectTransformer.transform(hubAssertion));

        return transformedAssertion;
    }

    private AttributeStatement transform(Cycle3Dataset cycle3Data) {
        AttributeStatement attributeStatement = openSamlXmlObjectFactory.createAttributeStatement();
        for (Map.Entry<String, String> entry : cycle3Data.getAttributes().entrySet()) {
            Attribute data = attributeFactory.createCycle3DataAttribute(entry.getKey(), entry.getValue());
            attributeStatement.getAttributes().add(data);

        }
        return attributeStatement;
    }
}
