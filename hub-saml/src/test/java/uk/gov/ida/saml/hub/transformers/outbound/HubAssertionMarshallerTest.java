package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.core.test.OpenSAMLExtension;
import uk.gov.ida.saml.core.transformers.outbound.OutboundAssertionToSubjectTransformer;
import uk.gov.ida.saml.hub.factories.AttributeFactory_1_1;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.Cycle3DatasetBuilder.aCycle3Dataset;
import static uk.gov.ida.saml.core.test.builders.HubAssertionBuilder.aHubAssertion;
import static uk.gov.ida.saml.core.test.builders.SimpleStringAttributeBuilder.aSimpleStringAttribute;

@ExtendWith(OpenSAMLExtension.class)
@ExtendWith(MockitoExtension.class)
public class HubAssertionMarshallerTest {

    private HubAssertionMarshaller marshaller;
    @Mock
    private AttributeFactory_1_1 attributeFactory = null;
    @Mock
    private OutboundAssertionToSubjectTransformer outboundAssertionToSubjectTransformer;

    @BeforeEach
    public void setup() {
        OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();
        marshaller = new HubAssertionMarshaller(openSamlXmlObjectFactory, attributeFactory, outboundAssertionToSubjectTransformer);
    }

    @Test
    public void transform_shouldTransformAssertionSubjects() {
        HubAssertion assertion = aHubAssertion().build();

        marshaller.toSaml(assertion);

        verify(outboundAssertionToSubjectTransformer).transform(assertion);
    }

    @Test
    public void transform_shouldTransformAssertionId() {
        String assertionId = "assertion-id";
        HubAssertion assertion = aHubAssertion().withId(assertionId).build();

        Assertion transformedAssertion = marshaller.toSaml(assertion);

        assertThat(transformedAssertion.getID()).isEqualTo(assertionId);
    }

    @Test
    public void transform_shouldTransformAssertionIssuer() {
        String assertionIssuerId = "assertion issuer";
        HubAssertion assertion = aHubAssertion().withIssuerId(assertionIssuerId).build();

        Assertion transformedAssertion = marshaller.toSaml(assertion);

        assertThat(transformedAssertion.getIssuer().getValue()).isEqualTo(assertionIssuerId);
    }

    @Test
    public void transform_shouldTransformAssertionIssuerInstance() {
        DateTime issueInstant = DateTime.parse("2012-12-31T12:34:56Z");
        HubAssertion assertion = aHubAssertion().withIssueInstant(issueInstant).build();

        Assertion transformedAssertion = marshaller.toSaml(assertion);

        assertThat(transformedAssertion.getIssueInstant()).isEqualTo(issueInstant);
    }

    @Test
    public void transform_shouldTransformCycle3DataAssertion() {
        String attributeName = "someName";
        String value = "some value";
        HubAssertion assertion = aHubAssertion().withCycle3Data(aCycle3Dataset().addCycle3Data(attributeName, value).build()).build();
        Attribute expectedAttribute = aSimpleStringAttribute().build();
        when(attributeFactory.createCycle3DataAttribute(attributeName, value)).thenReturn(expectedAttribute);

        Assertion transformedAssertion = marshaller.toSaml(assertion);

        List<AttributeStatement> attributeStatements = transformedAssertion.getAttributeStatements();
        assertThat(attributeStatements.size()).isGreaterThan(0);
        Attribute attribute = attributeStatements.get(0).getAttributes().get(0);
        assertThat(attribute).isEqualTo(expectedAttribute);
    }

    @Test
    public void transform_shouldTransformLevelOfCycle3DataAssertion() {
        String attributeName = "someName";
        String value = "some value";
        HubAssertion assertion = aHubAssertion().withCycle3Data(aCycle3Dataset().addCycle3Data(attributeName, value).build()).build();
        Attribute expectedAttribute = aSimpleStringAttribute().build();
        when(attributeFactory.createCycle3DataAttribute(attributeName, value)).thenReturn(expectedAttribute);

        Assertion transformedAssertion = marshaller.toSaml(assertion);

        List<AttributeStatement> attributeStatements = transformedAssertion.getAttributeStatements();
        assertThat(attributeStatements.size()).isGreaterThan(0);
        Attribute attribute = attributeStatements.get(0).getAttributes().get(0);
        assertThat(attribute).isEqualTo(expectedAttribute);
    }
}
