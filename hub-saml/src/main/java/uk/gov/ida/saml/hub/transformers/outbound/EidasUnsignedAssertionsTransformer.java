package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Conditions;
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
import uk.gov.ida.saml.core.transformers.AuthnContextFactory;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;

import java.util.List;
import java.util.UUID;

public class EidasUnsignedAssertionsTransformer {

    private final OpenSamlXmlObjectFactory openSamlXmlObjectFactory;
    private final AuthnContextFactory authnContextFactory;
    private final String hubEidasEntityId;

    public EidasUnsignedAssertionsTransformer(OpenSamlXmlObjectFactory openSamlXmlObjectFactory, AuthnContextFactory authnContextFactory, String hubEidasEntityId) {
        this.openSamlXmlObjectFactory = openSamlXmlObjectFactory;
        this.authnContextFactory = authnContextFactory;
        this.hubEidasEntityId = hubEidasEntityId;
    }

    public Assertion transform(HubEidasAttributeQueryRequest originalQuery) {

        CountrySignedResponseContainer countrySignedResponseContainer = originalQuery.getCountrySignedResponseContainer().get();
        Assertion assertion = openSamlXmlObjectFactory.createAssertion();
        DateTime now = DateTime.now();
        assertion.setIssueInstant(now);
        String unsignedCountryIssuer = countrySignedResponseContainer.getCountryEntityId();
        assertion.setIssuer(openSamlXmlObjectFactory.createIssuer(unsignedCountryIssuer));
        assertion.setID(UUID.randomUUID().toString());

        Conditions conditions = openSamlXmlObjectFactory.createConditions();
        conditions.setNotBefore(now);
        conditions.setNotOnOrAfter(now.plusMinutes(5));
        AudienceRestriction audienceRestriction = openSamlXmlObjectFactory.createAudienceRestriction(hubEidasEntityId);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        assertion.setConditions(conditions);

        Subject subject = openSamlXmlObjectFactory.createSubject();
        SubjectConfirmation subjectConfirmation = openSamlXmlObjectFactory.createSubjectConfirmation();
        SubjectConfirmationData subjectConfirmationData = createSubjectConfirmationData(originalQuery);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        subject.setNameID(openSamlXmlObjectFactory.createNameId(originalQuery.getPersistentId().getNameId()));
        assertion.setSubject(subject);

        AuthnStatement authnStatement = openSamlXmlObjectFactory.createAuthnStatement();
        AttributeStatement attributeStatement = openSamlXmlObjectFactory.createAttributeStatement();
        List<Attribute> attributes = attributeStatement.getAttributes();
        Attribute samlResponseAttribute = createCountrySamlResponseAttribute(countrySignedResponseContainer.getBase64SamlResponse());
        Attribute keysAttribute = createEncryptedAssertionKeysAttribute(countrySignedResponseContainer.getBase64encryptedKeys());
        attributes.add(samlResponseAttribute);
        attributes.add(keysAttribute);
        assertion.getAttributeStatements().add(attributeStatement);
        assertion.getAuthnStatements().add(authnStatement);
        setAuthnContextWithEidasLOA(originalQuery, authnStatement);
        return assertion;
    }

    private void setAuthnContextWithEidasLOA(HubEidasAttributeQueryRequest originalQuery, AuthnStatement authnStatement) {
        String levelOfAssurance = originalQuery.getAuthnContext().getUri();
        String eidasLOA = authnContextFactory.mapFromLoAToEidas(authnContextFactory.authnContextForLevelOfAssurance(levelOfAssurance));
        AuthnContext authnContext = openSamlXmlObjectFactory.createAuthnContext();
        AuthnContextClassRef authnContextClassReference = openSamlXmlObjectFactory.createAuthnContextClassReference(eidasLOA);
        authnContext.setAuthnContextClassRef(authnContextClassReference);
        authnStatement.setAuthnContext(authnContext);
    }

    private Attribute createCountrySamlResponseAttribute(String value) {
        CountrySamlResponse attributeValue = new CountrySamlResponseBuilder().buildObject();
        attributeValue.setValue(value);
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
            attributeValue.setValue(value);
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
