package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.extensions.eidas.CountrySamlResponse;
import uk.gov.ida.saml.core.extensions.eidas.EncryptedAssertionKeys;
import uk.gov.ida.saml.core.extensions.eidas.impl.CountrySamlResponseBuilder;
import uk.gov.ida.saml.core.extensions.eidas.impl.EncryptedAssertionKeysBuilder;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;

import java.util.List;
import java.util.UUID;

public class EidasUnsignedAssertionsTransformer {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;

    public EidasUnsignedAssertionsTransformer(OpenSamlXmlObjectFactory openSamlXmlObjectFactory) {

        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
    }

    public Assertion transform(HubEidasAttributeQueryRequest originalQuery) {

        CountrySignedResponseContainer countrySignedResponseContainer = originalQuery.getCountrySignedResponse().get();
        Assertion assertion = openSamlXmlObjectFactory.createAssertion();
        assertion.setIssueInstant(DateTime.now());
        String unsignedCountryIssuer = countrySignedResponseContainer.getCountryEntityId();
        assertion.setIssuer(openSamlXmlObjectFactory.createIssuer(unsignedCountryIssuer));
        assertion.setID(UUID.randomUUID().toString());

        Subject newSub = openSamlXmlObjectFactory.createSubject();
        SubjectConfirmation subjectConfirmation = openSamlXmlObjectFactory.createSubjectConfirmation();
        SubjectConfirmationData subjectConfirmationData = createSubjectConfirmationData(originalQuery);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        newSub.getSubjectConfirmations().add(subjectConfirmation);
        newSub.setNameID(openSamlXmlObjectFactory.createNameId(originalQuery.getPersistentId().getNameId()));
        assertion.setSubject(newSub);

        AuthnStatement authnStatement = openSamlXmlObjectFactory.createAuthnStatement();
        AttributeStatement attributeStatement = openSamlXmlObjectFactory.createAttributeStatement();
        List<Attribute> attributes = attributeStatement.getAttributes();
        Attribute samlResponseAttribute = createCountrySamlResponseAttribute(countrySignedResponseContainer.getBase64SamlResponse());
        Attribute keysAttribute = createEncryptedAssertionKeysAttribute(countrySignedResponseContainer.getBase64encryptedKeys());
        attributes.add(samlResponseAttribute);
        attributes.add(keysAttribute);
        assertion.getAttributeStatements().add(attributeStatement);
        assertion.getAuthnStatements().add(authnStatement);

        return assertion;
    }

    private Attribute createCountrySamlResponseAttribute(String value) {
        CountrySamlResponse attributeValue = new CountrySamlResponseBuilder().buildObject();
        attributeValue.setCountrySamlResponse(value);
        Attribute attribute = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName(IdaConstants.Eidas_Attributes.UnsignedAssertions.EidasSamlResponse.NAME);
        attribute.setFriendlyName(IdaConstants.Eidas_Attributes.UnsignedAssertions.EidasSamlResponse.FRIENDLY_NAME);
        attribute.setNameFormat(Attribute.URI_REFERENCE);
        attribute.getAttributeValues().add(attributeValue);
        return attribute;
    }

    private Attribute createEncryptedAssertionKeysAttribute(List<String> values) {
        Attribute attribute = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName(IdaConstants.Eidas_Attributes.UnsignedAssertions.EncryptedSecretKeys.NAME);
        attribute.setFriendlyName(IdaConstants.Eidas_Attributes.UnsignedAssertions.EncryptedSecretKeys.FRIENDLY_NAME);
        attribute.setNameFormat(Attribute.URI_REFERENCE);
        EncryptedAssertionKeysBuilder encryptedAssertionKeysBuilder = new EncryptedAssertionKeysBuilder();
        values.forEach(value -> {
            EncryptedAssertionKeys attributeValue = encryptedAssertionKeysBuilder.buildObject();
            attributeValue.setEncryptedAssertionKeys(value);
            attribute.getAttributeValues().add(attributeValue);
        });
        return attribute;
    }

    private SubjectConfirmationData createSubjectConfirmationData(HubEidasAttributeQueryRequest request) {
        SubjectConfirmationData subjectConfirmationData = openSamlXmlObjectFactory.createSubjectConfirmationData();
        subjectConfirmationData.setInResponseTo(request.getId());
        subjectConfirmationData.setNotOnOrAfter(DateTime.now().plusMinutes(15));
        subjectConfirmationData.setNotBefore(DateTime.now());
        return subjectConfirmationData;
    }
}
