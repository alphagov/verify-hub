package uk.gov.ida.saml.core.validators.assertion;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.IdpFraudEventId;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.validation.SamlTransformationErrorException;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidAttributeLanguageInAssertion;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidFraudAttribute;

public class AssertionAttributeStatementValidator {

    public static final String INVALID_FRAUD_EVENT_TYPE = "Invalid fraud event type";
    private static final String INVALID_FRAUD_EVENT_NAME = "Invalid fraud event name";
    private static final String INVALID_NUMBER_OF_FRAUD_EVENT_ATTRIBUTE_STATEMENTS = "Invalid number of fraud event attribute statements";

    public void validate(Assertion assertion) {
        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                for (XMLObject attributeValue : attribute.getAttributeValues()) {
                    if (attributeValue instanceof PersonName) {
                        PersonName personName = (PersonName) attributeValue;
                        String language = personName.getLanguage();
                        if (language != null && !IdaConstants.IDA_LANGUAGE.equals(language)) {
                            SamlValidationSpecificationFailure failure = invalidAttributeLanguageInAssertion(attribute.getName(), language);
                            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
                        }
                    }
                }
            }
        }
    }

    public void validateFraudEvent(Assertion assertion) {
        if(assertion.getAttributeStatements().size() != 1){
            SamlValidationSpecificationFailure failure = invalidFraudAttribute(INVALID_NUMBER_OF_FRAUD_EVENT_ATTRIBUTE_STATEMENTS);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        }
        else {
            AttributeStatement attributeStatement = assertion.getAttributeStatements().get(0);
            validateFraudEvent(attributeStatement);
        }
    }

    private void validateFraudEvent(AttributeStatement attributeStatement) {
        Attribute fraudEventAttribute = Iterables.find(attributeStatement.getAttributes(), new Predicate<Attribute>() {
            @Override
            public boolean apply(Attribute input) {
                return input.getName().equals(IdaConstants.Attributes_1_1.IdpFraudEventId.NAME);
            }
        }, null);
        if(fraudEventAttribute == null){
            SamlValidationSpecificationFailure failure = invalidFraudAttribute(INVALID_FRAUD_EVENT_NAME);
            throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
        } else {
            boolean didNotDeserializeCorrectlyIntoFraudEventType = !(fraudEventAttribute.getAttributeValues().get(0) instanceof IdpFraudEventId);
            if (didNotDeserializeCorrectlyIntoFraudEventType) {
                SamlValidationSpecificationFailure failure = invalidFraudAttribute(INVALID_FRAUD_EVENT_TYPE);
                throw new SamlTransformationErrorException(failure.getErrorMessage(), failure.getLogLevel());
            }
        }
    }


}
