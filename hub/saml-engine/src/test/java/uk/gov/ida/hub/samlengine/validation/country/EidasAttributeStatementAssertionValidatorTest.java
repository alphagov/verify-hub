package uk.gov.ida.hub.samlengine.validation.country;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.IdaConstants.Eidas_Attributes;
import uk.gov.ida.saml.core.extensions.eidas.CurrentFamilyName;
import uk.gov.ida.saml.core.extensions.eidas.CurrentGivenName;
import uk.gov.ida.saml.core.extensions.eidas.DateOfBirth;
import uk.gov.ida.saml.core.extensions.eidas.PersonIdentifier;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;

import javax.xml.namespace.QName;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EidasAttributeStatementAssertionValidatorTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Mock
    private Assertion assertion;
    @Mock
    private AttributeStatement attributeStatement;
    private Attribute firstName;
    private Attribute lastName;
    private Attribute dateOfBirth;
    private Attribute personIdentifier;

    private EidasAttributeStatementAssertionValidator validator;

    @Before
    public void setup() {
        firstName = generateAttribute(Eidas_Attributes.FirstName.NAME, Eidas_Attributes.FirstName.FRIENDLY_NAME, CurrentGivenName.TYPE_NAME);
        lastName = generateAttribute(Eidas_Attributes.FamilyName.NAME, Eidas_Attributes.FamilyName.FRIENDLY_NAME, CurrentFamilyName.TYPE_NAME);
        dateOfBirth = generateAttribute(Eidas_Attributes.DateOfBirth.NAME, Eidas_Attributes.DateOfBirth.FRIENDLY_NAME, DateOfBirth.TYPE_NAME);
        personIdentifier = generateAttribute(Eidas_Attributes.PersonIdentifier.NAME, Eidas_Attributes.PersonIdentifier.FRIENDLY_NAME, PersonIdentifier.TYPE_NAME);
        validator = new EidasAttributeStatementAssertionValidator();
        when(assertion.getAttributeStatements()).thenReturn(ImmutableList.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(ImmutableList.of(firstName, lastName, dateOfBirth, personIdentifier));
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfNoAttributeStatements() {
        when(assertion.getAttributeStatements()).thenReturn(Collections.emptyList());

        validator.validate(assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfMultipleAttributeStatements() {
        when(assertion.getAttributeStatements()).thenReturn(ImmutableList.of(attributeStatement, attributeStatement));

        validator.validate(assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfAttributeHasInvalidName() {
        when(firstName.getName()).thenReturn("invalid");

        validator.validate(assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfAttributeHasNoValues() {
        when(firstName.getAttributeValues()).thenReturn(Collections.emptyList());

        validator.validate(assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfAttributeValueHasInvalidSchemaType() {
        XMLObject xmlObject = mock(XMLObject.class);
        when(firstName.getAttributeValues()).thenReturn(ImmutableList.of(xmlObject));
        when(xmlObject.getSchemaType()).thenReturn(CurrentFamilyName.TYPE_NAME);

        validator.validate(assertion);
    }

    @Test
    public void shouldInsistOnMandatoryAttributes() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Mandatory attributes not provided.");

        when(attributeStatement.getAttributes()).thenReturn(ImmutableList.of(firstName, lastName, dateOfBirth));

        validator.validate(assertion);
    }

    private Attribute generateAttribute(String name, String friendlyName, QName schemaType) {
        Attribute attribute = mock(Attribute.class);
        XMLObject xmlObject = mock(XMLObject.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getFriendlyName()).thenReturn(friendlyName);
        when(attribute.getAttributeValues()).thenReturn(ImmutableList.of(xmlObject));
        return attribute;
    }

}
