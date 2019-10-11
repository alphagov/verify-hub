package uk.gov.ida.saml.hub.transformers.outbound;

import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.domain.CountrySignedResponseContainer;
import uk.gov.ida.saml.core.domain.HubAssertion;
import uk.gov.ida.saml.hub.domain.HubEidasAttributeQueryRequest;
import uk.gov.ida.saml.hub.domain.UserAccountCreationAttribute;
import uk.gov.ida.saml.hub.factories.AttributeQueryAttributeFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


public class HubEidasAttributeQueryRequestToSamlAttributeQueryTransformer implements Function<HubEidasAttributeQueryRequest,AttributeQuery> {

    private final OpenSamlXmlObjectFactory samlObjectFactory;
    private final HubAssertionMarshaller hubAssertionMarshaller;
    private final AssertionFromIdpToAssertionTransformer assertionFromIdpTransformer;
    private final AttributeQueryAttributeFactory attributeQueryAttributeFactory;
    private final EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller;
    private final EidasUnsignedAssertionsTransformer eidasUnsignedAssertionsTransformer;
    private final SamlAttributeQueryAssertionSignatureSigner signatureSigner;

    public HubEidasAttributeQueryRequestToSamlAttributeQueryTransformer(
            final OpenSamlXmlObjectFactory samlObjectFactory,
            final HubAssertionMarshaller hubAssertionMarshaller,
            final AssertionFromIdpToAssertionTransformer assertionFromIdpTransformer,
            final AttributeQueryAttributeFactory attributeQueryAttributeFactory,
            final EncryptedAssertionUnmarshaller encryptedAssertionUnmarshaller,
            EidasUnsignedAssertionsTransformer eidasUnsignedAssertionsTransformer,
            SamlAttributeQueryAssertionSignatureSigner signatureSigner) {

        this.samlObjectFactory = samlObjectFactory;
        this.hubAssertionMarshaller = hubAssertionMarshaller;
        this.assertionFromIdpTransformer = assertionFromIdpTransformer;
        this.attributeQueryAttributeFactory = attributeQueryAttributeFactory;
        this.encryptedAssertionUnmarshaller = encryptedAssertionUnmarshaller;
        this.eidasUnsignedAssertionsTransformer = eidasUnsignedAssertionsTransformer;
        this.signatureSigner = signatureSigner;
    }

    public AttributeQuery apply(HubEidasAttributeQueryRequest originalQuery) {
        AttributeQuery transformedQuery = samlObjectFactory.createAttributeQuery();

        Issuer issuer = samlObjectFactory.createIssuer(originalQuery.getIssuer());

        transformedQuery.setID(originalQuery.getId());
        transformedQuery.setIssuer(issuer);
        transformedQuery.setIssueInstant(DateTime.now());

        if (originalQuery.getUserAccountCreationAttributes().isPresent()){
            transformedQuery.getAttributes().addAll(createAttributeList(originalQuery.getUserAccountCreationAttributes().get()));
        }

        Subject subject = samlObjectFactory.createSubject();
        NameID nameId = samlObjectFactory.createNameId(originalQuery.getPersistentId().getNameId());
        nameId.setSPNameQualifier(originalQuery.getAuthnRequestIssuerEntityId());
        nameId.setNameQualifier(originalQuery.getAssertionConsumerServiceUrl().toASCIIString());
        subject.setNameID(nameId);

        SubjectConfirmation subjectConfirmation = samlObjectFactory.createSubjectConfirmation();
        SubjectConfirmationData subjectConfirmationData = samlObjectFactory.createSubjectConfirmationData();

        final Optional<HubAssertion> cycle3DatasetAssertion = originalQuery.getCycle3AttributeAssertion();
        if (cycle3DatasetAssertion.isPresent()) {
            Assertion transformedCycle3DatasetAssertion = hubAssertionMarshaller.toSaml(cycle3DatasetAssertion.get());
            subjectConfirmationData.getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME).add(transformedCycle3DatasetAssertion);
        }

        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        transformedQuery.setSubject(subject);

        Optional<CountrySignedResponseContainer> countrySignedResponseContainer = originalQuery.getCountrySignedResponseContainer();
        if (countrySignedResponseContainer.isPresent()) {
            Assertion unsignedAssertion = eidasUnsignedAssertionsTransformer.transform(originalQuery);
            subjectConfirmationData.getUnknownXMLObjects(Assertion.DEFAULT_ELEMENT_NAME).add(unsignedAssertion);
            signatureSigner.signAssertions(transformedQuery);
        } else {
            final String encryptedIdentityAssertion = originalQuery.getEncryptedIdentityAssertion();
            EncryptedAssertion encryptedAssertion = encryptedAssertionUnmarshaller.transform(encryptedIdentityAssertion);
            subjectConfirmationData.getUnknownXMLObjects(EncryptedAssertion.DEFAULT_ELEMENT_NAME).add(encryptedAssertion);
        }
        return transformedQuery;
    }

    private List<Attribute> createAttributeList(List<UserAccountCreationAttribute> userAccountCreationAttributes) {
        return userAccountCreationAttributes
                .stream()
                .map(attributeQueryAttributeFactory::createAttribute)
                .collect(Collectors.toList());
    }
}
