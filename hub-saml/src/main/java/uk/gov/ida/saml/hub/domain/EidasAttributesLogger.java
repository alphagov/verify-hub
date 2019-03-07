package uk.gov.ida.saml.hub.domain;

import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.BaseMdsSamlObject;
import uk.gov.ida.saml.core.transformers.EidasResponseAttributesHashLogger;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EidasAttributesLogger {

    private final Supplier<EidasResponseAttributesHashLogger> loggerSupplier;
    private final Optional<String> hubEidasEntityId;

    public EidasAttributesLogger(Supplier<EidasResponseAttributesHashLogger> loggerSupplier, Optional<String> hubEidasEntityId) {
        this.loggerSupplier = loggerSupplier;
        this.hubEidasEntityId = hubEidasEntityId;
    }

    public boolean isEidasJourney(String matchingServiceEntityId) {
        return hubEidasEntityId.isPresent() && hubEidasEntityId.get().equals(matchingServiceEntityId);
    }

    public void logEidasAttributesAsHash(Assertion assertion, Response response) {

        Subject subject = assertion.getSubject();
        String pid = subject.getNameID().getValue();
        EidasResponseAttributesHashLogger hashLogger = loggerSupplier.get();
        hashLogger.setPid(pid);

        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();

        attributeStatements.stream().findFirst().ifPresent(
                attributeStatement -> attributeStatement.getAttributes().forEach(
                        attribute -> {

                            switch (attribute.getName()) {

                                case IdaConstants.Attributes_1_1.Firstname.NAME:
                                    setFirstVerified(hashLogger, attribute, v -> hashLogger.setFirstName(getTextContent(v)));
                                    break;
                                case IdaConstants.Attributes_1_1.Middlename.NAME:
                                    attribute.getAttributeValues().forEach(v -> hashLogger.addMiddleName(getTextContent(v)));
                                    break;
                                case IdaConstants.Attributes_1_1.Surname.NAME:
                                    attribute.getAttributeValues().forEach(v -> hashLogger.addSurname(getTextContent(v)));
                                    break;
                                case IdaConstants.Attributes_1_1.DateOfBirth.NAME:
                                    setFirstVerified(hashLogger, attribute, v -> hashLogger.setDateOfBirth(DateTime.parse(getTextContent(v))));
                                    break;
                                default:
                                    break;
                            }
                        }));

        hashLogger.logHashFor(response.getID(), response.getDestination());
    }

    private void setFirstVerified(EidasResponseAttributesHashLogger hashLogger, Attribute attribute, Consumer<XMLObject> xmlObjectConsumer) {
        attribute.getAttributeValues()
                .stream()
                .map(BaseMdsSamlObject.class::cast)
                .filter(BaseMdsSamlObject::getVerified)
                .map(XMLObject.class::cast)
                .findFirst()
                .ifPresent(xmlObjectConsumer);
    }

    private String getTextContent(XMLObject v) {
        return v.getDOM().getTextContent();
    }
}
