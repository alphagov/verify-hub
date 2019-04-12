package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.domain.AuthnContext;
import uk.gov.ida.saml.core.extensions.BaseMdsSamlObject;
import uk.gov.ida.saml.core.transformers.EidasResponseAttributesHashLogger;
import uk.gov.ida.saml.hub.factories.UserIdHashFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EidasAttributesLogger {
    private final Supplier<EidasResponseAttributesHashLogger> loggerSupplier;
    private final UserIdHashFactory userIdHashFactory;

    public EidasAttributesLogger(Supplier<EidasResponseAttributesHashLogger> loggerSupplier, String hashingEntityId) {
        this.loggerSupplier = loggerSupplier;
        this.userIdHashFactory = new UserIdHashFactory(hashingEntityId);
    }

    public void logEidasAttributesAsHash(Assertion assertion, Response response) {

        Subject subject = assertion.getSubject();
        String persistentId = subject.getNameID().getValue();
        EidasResponseAttributesHashLogger hashLogger = loggerSupplier.get();

        String hashedPid = userIdHashFactory.hashId(
            assertion.getIssuer().getValue(),
            persistentId,
            Optional.of(AuthnContext.LEVEL_2));

        hashLogger.setPid(hashedPid);

        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        List<Attribute> attributes = attributeStatements.get(0).getAttributes();
        for (Attribute attribute : attributes) {
            switch (attribute.getName()) {

                case IdaConstants.Attributes_1_1.Firstname.NAME:
                    logFirstVerified(attribute, v -> hashLogger.setFirstName(getTextContent(v)));
                    break;
                case IdaConstants.Attributes_1_1.Middlename.NAME:
                    attribute.getAttributeValues().forEach(v -> hashLogger.addMiddleName(getTextContent(v)));
                    break;
                case IdaConstants.Attributes_1_1.Surname.NAME:
                    attribute.getAttributeValues().forEach(v -> hashLogger.addSurname(getTextContent(v)));
                    break;
                case IdaConstants.Attributes_1_1.DateOfBirth.NAME:
                    logFirstVerified(attribute, v -> hashLogger.setDateOfBirth(DateTime.parse(getTextContent(v))));
                    break;
                default:
                    break;
            }
        }

        hashLogger.logHashFor(response.getInResponseTo(), response.getDestination());
    }

    private void logFirstVerified(Attribute attribute, Consumer<XMLObject> xmlObjectConsumer) {
        for (XMLObject attributeValue : attribute.getAttributeValues()) {
            if (attributeValue instanceof BaseMdsSamlObject) {
                if (((BaseMdsSamlObject) attributeValue).getVerified()) {
                    xmlObjectConsumer.accept(attributeValue);
                    return;
                }
            }
        }
    }

    private String getTextContent(XMLObject xmlObject) {
        return xmlObject.getDOM().getTextContent();
    }
}
