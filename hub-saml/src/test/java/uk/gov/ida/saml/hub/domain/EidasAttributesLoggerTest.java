package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.w3c.dom.Element;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.Date;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.transformers.EidasResponseAttributesHashLogger;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EidasAttributesLoggerTest {

    @Mock
    private EidasResponseAttributesHashLogger hashLogger;

    @Mock
    private Assertion assertion;

    @Mock
    private Issuer issuer;

    @Mock
    private Subject subject;

    @Mock
    private NameID nameID;

    @Mock
    private Response response;

    @Mock
    private AttributeStatement attributeStatement;

    private String entityId = "entity-id";
    private String hashedPid = "0b0cefe3c464c0c3f928d58d1f7fef468e3ff415428e1c0b828d7a78da4c7b21";

    @Before
    public void setUp() throws Exception {
        when(assertion.getSubject()).thenReturn(subject);
        when(assertion.getIssuer()).thenReturn(issuer);
        when(issuer.getValue()).thenReturn("issuer");
        when(subject.getNameID()).thenReturn(nameID);
        when(nameID.getValue()).thenReturn("pid");
        when(response.getInResponseTo()).thenReturn("request id");
        when(response.getDestination()).thenReturn("destination");
    }

    @Test
    public void testInResponseToOnIDPResponseIsUsedAsHubRequestIdInEDIASHashLogging() {
        EidasAttributesLogger eidasAttributesLogger = new EidasAttributesLogger(() -> hashLogger, entityId);
        PersonName attributeValue = mock(PersonName.class);
        when(attributeValue.getVerified()).thenReturn(true);
        Element element = mock(Element.class);
        when(element.getTextContent()).thenReturn("Paul");
        when(attributeValue.getDOM()).thenReturn(element);
        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(IdaConstants.Attributes_1_1.Firstname.NAME);
        when(attribute.getAttributeValues()).thenReturn(List.of(attributeValue));
        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(attribute));
        eidasAttributesLogger.logEidasAttributesAsHash(assertion, response);
        verify(response).getInResponseTo();
        verify(response).getDestination();
        verifyNoMoreInteractions(response);
    }


    @Test
    public void testOnlyFirstValidFirstNameIsHashed() {
        EidasAttributesLogger eidasAttributesLogger = new EidasAttributesLogger(() -> hashLogger, entityId);

        PersonName attributeValue0 = mock(PersonName.class);
        PersonName attributeValue1 = mock(PersonName.class);
        PersonName attributeValue2 = mock(PersonName.class);

        when(attributeValue0.getVerified()).thenReturn(false);
        when(attributeValue1.getVerified()).thenReturn(true);

        Element element1 = mock(Element.class);
        when(element1.getTextContent()).thenReturn("Paul");
        when(attributeValue1.getDOM()).thenReturn(element1);

        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(IdaConstants.Attributes_1_1.Firstname.NAME);
        when(attribute.getAttributeValues()).thenReturn(List.of(attributeValue0, attributeValue1, attributeValue2));

        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(attribute));
        eidasAttributesLogger.logEidasAttributesAsHash(assertion, response);
        verify(hashLogger).setPid(hashedPid);
        verify(hashLogger).setFirstName("Paul");
        verify(hashLogger).logHashFor("request id", "destination");

        verify(attributeValue0).getVerified();
        verify(attributeValue1).getVerified();
        verify(attributeValue2, never()).getVerified();
        verify(attributeValue1).getDOM();
        verify(attributeValue0, never()).getDOM();
        verify(attributeValue2, never()).getDOM();
        verifyNoMoreInteractions(hashLogger);
    }

    @Test
    public void testUnverifiedFirstNamesNeverLogged() {
        EidasAttributesLogger eidasAttributesLogger = new EidasAttributesLogger(() -> hashLogger, entityId);

        PersonName attributeValue0 = mock(PersonName.class);
        PersonName attributeValue1 = mock(PersonName.class);

        when(attributeValue0.getVerified()).thenReturn(false);
        when(attributeValue1.getVerified()).thenReturn(false);

        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(IdaConstants.Attributes_1_1.Firstname.NAME);
        when(attribute.getAttributeValues()).thenReturn(List.of(attributeValue0, attributeValue1));

        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(attribute));

        eidasAttributesLogger.logEidasAttributesAsHash(assertion, response);

        verify(hashLogger).setPid(hashedPid);
        verify(hashLogger).logHashFor("request id", "destination");
        verify(attributeValue0).getVerified();
        verify(attributeValue1).getVerified();
        verify(attributeValue1, never()).getDOM();
        verify(attributeValue0, never()).getDOM();
        verifyNoMoreInteractions(hashLogger);
    }

    @Test
    public void testOnlyFirstValidDateOfBirthIsHashed() {
        EidasAttributesLogger eidasAttributesLogger = new EidasAttributesLogger(() -> hashLogger, entityId);

        Date attributeValue0 = mock(Date.class);
        Date attributeValue1 = mock(Date.class);
        Date attributeValue2 = mock(Date.class);
        when(attributeValue0.getVerified()).thenReturn(false);
        when(attributeValue1.getVerified()).thenReturn(true);
        Element element = mock(Element.class);
        when(element.getTextContent()).thenReturn("2000-01-25");
        when(attributeValue1.getDOM()).thenReturn(element);

        Attribute attribute = mock(Attribute.class);
        when(attribute.getName()).thenReturn(IdaConstants.Attributes_1_1.DateOfBirth.NAME);
        when(attribute.getAttributeValues()).thenReturn(List.of(attributeValue0, attributeValue1, attributeValue2));

        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(attribute));
        eidasAttributesLogger.logEidasAttributesAsHash(assertion, response);
        verify(hashLogger).setPid(hashedPid);
        verify(hashLogger).setDateOfBirth(DateTime.parse("2000-01-25"));
        verify(hashLogger).logHashFor("request id", "destination");
        verify(attributeValue0).getVerified();
        verify(attributeValue1).getVerified();
        verify(attributeValue2, never()).getVerified();
        verify(attributeValue1).getDOM();
        verify(attributeValue0, never()).getDOM();
        verify(attributeValue2, never()).getDOM();
        verifyNoMoreInteractions(hashLogger);
    }

    @Test
    public void testAllMiddleNamesHashedInCorrectOrder() {
        EidasAttributesLogger eidasAttributesLogger = new EidasAttributesLogger(() -> hashLogger, entityId);

        PersonName attributeValue0 = mock(PersonName.class);
        PersonName attributeValue1 = mock(PersonName.class);
        Element element0 = mock(Element.class);
        Element element1 = mock(Element.class);
        when(element0.getTextContent()).thenReturn("middle name 0");
        when(element1.getTextContent()).thenReturn("middle name 1");
        when(attributeValue0.getDOM()).thenReturn(element0);
        when(attributeValue1.getDOM()).thenReturn(element1);

        Attribute attributeMiddleName = mock(Attribute.class);
        when(attributeMiddleName.getName()).thenReturn(IdaConstants.Attributes_1_1.Middlename.NAME);
        when(attributeMiddleName.getAttributeValues()).thenReturn(List.of(attributeValue0, attributeValue1));

        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(attributeMiddleName));
        eidasAttributesLogger.logEidasAttributesAsHash(assertion, response);
        verify(hashLogger).setPid(hashedPid);

        InOrder inOrder = inOrder(hashLogger);
        inOrder.verify(hashLogger).addMiddleName("middle name 0");
        inOrder.verify(hashLogger).addMiddleName("middle name 1");
        verify(hashLogger).logHashFor("request id", "destination");
        verify(attributeValue0, never()).getVerified();
        verify(attributeValue1, never()).getVerified();
        verifyNoMoreInteractions(hashLogger);

    }

    @Test
    public void testAllSurnamesHashedInCorrectOrder() {
        EidasAttributesLogger eidasAttributesLogger = new EidasAttributesLogger(() -> hashLogger, entityId);

        PersonName attributeValue0 = mock(PersonName.class);
        PersonName attributeValue1 = mock(PersonName.class);
        Element element0 = mock(Element.class);
        Element element1 = mock(Element.class);
        when(element0.getTextContent()).thenReturn("surname 0");
        when(element1.getTextContent()).thenReturn("surname 1");
        when(attributeValue0.getDOM()).thenReturn(element0);
        when(attributeValue1.getDOM()).thenReturn(element1);

        Attribute attributeSurname = mock(Attribute.class);
        when(attributeSurname.getName()).thenReturn(IdaConstants.Attributes_1_1.Surname.NAME);
        when(attributeSurname.getAttributeValues()).thenReturn(List.of(attributeValue0, attributeValue1));

        when(assertion.getAttributeStatements()).thenReturn(List.of(attributeStatement));
        when(attributeStatement.getAttributes()).thenReturn(List.of(attributeSurname));
        eidasAttributesLogger.logEidasAttributesAsHash(assertion, response);
        verify(hashLogger).setPid(hashedPid);

        InOrder inOrder = inOrder(hashLogger);
        inOrder.verify(hashLogger).addSurname("surname 0");
        inOrder.verify(hashLogger).addSurname("surname 1");
        verify(hashLogger).logHashFor("request id", "destination");
        verify(attributeValue0, never()).getVerified();
        verify(attributeValue1, never()).getVerified();
        verifyNoMoreInteractions(hashLogger);

    }


}