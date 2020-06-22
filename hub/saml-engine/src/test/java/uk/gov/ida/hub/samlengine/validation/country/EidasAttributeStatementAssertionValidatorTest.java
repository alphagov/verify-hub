package uk.gov.ida.hub.samlengine.validation.country;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.core.xml.Namespace;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.IdaConstants.Eidas_Attributes;
import uk.gov.ida.saml.core.extensions.eidas.CurrentFamilyName;
import uk.gov.ida.saml.core.extensions.eidas.CurrentGivenName;
import uk.gov.ida.saml.core.extensions.eidas.DateOfBirth;
import uk.gov.ida.saml.core.extensions.eidas.PersonIdentifier;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.security.validators.ValidatedResponse;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EidasAttributeStatementAssertionValidatorTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Mock
    private Assertion assertion;
    @Mock
    private ValidatedResponse validatedResponse;
    @Mock
    private AttributeStatement attributeStatement;
    private Attribute firstName;
    private Attribute lastName;
    private Attribute dateOfBirth;
    private Attribute personIdentifier;

    private EidasAttributeStatementAssertionValidator validator;

    @Before
    public void setup() {
        firstName = generateAttribute(Eidas_Attributes.FirstName.NAME, Eidas_Attributes.FirstName.FRIENDLY_NAME, new QName(
                XMLConstants.NULL_NS_URI,
                CurrentGivenName.TYPE_LOCAL_NAME,
                IdaConstants.EIDAS_NATURUAL_PREFIX));
        lastName = generateAttribute(Eidas_Attributes.FamilyName.NAME, Eidas_Attributes.FamilyName.FRIENDLY_NAME, CurrentFamilyName.TYPE_NAME);
        dateOfBirth = generateAttribute(Eidas_Attributes.DateOfBirth.NAME, Eidas_Attributes.DateOfBirth.FRIENDLY_NAME, DateOfBirth.TYPE_NAME);
        personIdentifier = generateAttribute(Eidas_Attributes.PersonIdentifier.NAME, Eidas_Attributes.PersonIdentifier.FRIENDLY_NAME, PersonIdentifier.TYPE_NAME);
        validator = new EidasAttributeStatementAssertionValidator();
        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(firstName, lastName, dateOfBirth, personIdentifier));
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfNoAttributeStatements() {
        when(assertion.getAttributeStatements()).thenReturn(Collections.emptyList());

        validator.validate(validatedResponse, assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfMultipleAttributeStatements() {
        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement, attributeStatement));

        validator.validate(validatedResponse, assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfAttributeHasInvalidName() {
        when(firstName.getName()).thenReturn("invalid");

        validator.validate(validatedResponse, assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfAttributeHasNoValues() {
        when(firstName.getAttributeValues()).thenReturn(Collections.emptyList());

        validator.validate(validatedResponse, assertion);
    }

    @Test(expected = SamlTransformationErrorException.class)
    public void shouldThrowIfAttributeValueHasInvalidSchemaType() {
        XMLObject xmlObject = mock(XMLObject.class);
        when(firstName.getAttributeValues()).thenReturn(List.of(xmlObject));
        when(xmlObject.getSchemaType()).thenReturn(CurrentFamilyName.TYPE_NAME);

        validator.validate(validatedResponse, assertion);
    }

    @Test
    public void shouldInsistOnMandatoryAttributes() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Mandatory attributes not provided.");

        when(attributeStatement.getAttributes()).thenReturn(List.of(firstName, lastName, dateOfBirth));

        validator.validate(validatedResponse, assertion);
    }

    @Test
    public void validateCorrectNamespaceURIOnResponseAllowsValidMatchOnAttribute() {
        when(validatedResponse.getNamespaces()).thenReturn(Set.of(new Namespace(
                IdaConstants.EIDAS_NATURAL_PERSON_NS,
                IdaConstants.EIDAS_NATURUAL_PREFIX)));

        attributeStatement.getAttributes().forEach(a -> when(a.getNameFormat()).thenReturn(Attribute.URI_REFERENCE));

        validator.validate(validatedResponse, assertion);

        verify(validatedResponse, times(1)).getNamespaces();

    }

    @Test
    public void validateIncorrectNamespaceURIOnResponseWhenNoneOnAttributeValueThrowsException() {
        exception.expect(SamlTransformationErrorException.class);
        exception.expectMessage("Attribute 'http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName' has incorrect type");

        when(validatedResponse.getNamespaces()).thenReturn(Set.of(new Namespace(
                "some other namespace uri",
                IdaConstants.EIDAS_NATURUAL_PREFIX)));

        validator.validate(validatedResponse, assertion);

    }


    private Attribute generateAttribute(String name, String friendlyName, QName schemaType) {
        Attribute attribute = mock(Attribute.class);
        AttributeValue attributeValue = mock(AttributeValue.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getFriendlyName()).thenReturn(friendlyName);
        when(attribute.getAttributeValues()).thenReturn(List.of(attributeValue));
        when(attributeValue.getSchemaType()).thenReturn(schemaType);
        return attribute;
    }

}
