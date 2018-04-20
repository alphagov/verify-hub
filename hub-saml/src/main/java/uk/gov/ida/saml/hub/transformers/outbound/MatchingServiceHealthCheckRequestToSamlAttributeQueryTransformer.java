package uk.gov.ida.saml.hub.transformers.outbound;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.hub.domain.MatchingServiceHealthCheckRequest;

import java.util.function.Function;

public class MatchingServiceHealthCheckRequestToSamlAttributeQueryTransformer implements Function<MatchingServiceHealthCheckRequest,AttributeQuery> {

    private final OpenSamlXmlObjectFactory samlObjectFactory;

    @Inject
    public MatchingServiceHealthCheckRequestToSamlAttributeQueryTransformer(OpenSamlXmlObjectFactory samlObjectFactory) {
        this.samlObjectFactory = samlObjectFactory;
    }

    public AttributeQuery apply(MatchingServiceHealthCheckRequest originalQuery) {
        AttributeQuery transformedQuery = samlObjectFactory.createAttributeQuery();

        Issuer issuer = samlObjectFactory.createIssuer(originalQuery.getIssuer());

        transformedQuery.setID(originalQuery.getId());
        transformedQuery.setIssuer(issuer);
        transformedQuery.setIssueInstant(DateTime.now());

        Subject subject = samlObjectFactory.createSubject();
        NameID nameId = samlObjectFactory.createNameId(originalQuery.getPersistentId().getNameId());
        nameId.setSPNameQualifier(originalQuery.getAuthnRequestIssuerEntityId());
        nameId.setNameQualifier(originalQuery.getAssertionConsumerServiceUrl().toASCIIString());
        subject.setNameID(nameId);

        SubjectConfirmation subjectConfirmation = samlObjectFactory.createSubjectConfirmation();
        SubjectConfirmationData subjectConfirmationData = samlObjectFactory.createSubjectConfirmationData();

        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subject.getSubjectConfirmations().add(subjectConfirmation);

        transformedQuery.setSubject(subject);

        return transformedQuery;
    }

}
