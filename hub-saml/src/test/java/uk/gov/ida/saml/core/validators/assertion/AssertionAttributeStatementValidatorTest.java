package uk.gov.ida.saml.core.validators.assertion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import uk.gov.ida.saml.core.OpenSamlXmlObjectFactory;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.AssertionBuilder;
import uk.gov.ida.saml.core.test.builders.IdpFraudEventIdAttributeBuilder;

import static java.util.Arrays.asList;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidAttributeLanguageInAssertion;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.invalidFraudAttribute;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.SimpleStringAttributeBuilder.aSimpleStringAttribute;

@RunWith(OpenSAMLMockitoRunner.class)
public class AssertionAttributeStatementValidatorTest {

    private AssertionAttributeStatementValidator validator;

    private OpenSamlXmlObjectFactory openSamlXmlObjectFactory = new OpenSamlXmlObjectFactory();

    @Before
    public void setUp() throws Exception {
        validator = new AssertionAttributeStatementValidator();
    }

    @Test
    public void validate_shouldThrowWhenAttributesIsNotEnGb() throws Exception {
        Attribute validAttributeOne = aSimpleStringAttribute().build();
        PersonName validFirstNameAttribute = openSamlXmlObjectFactory.createPersonNameAttributeValue("Dave");
        validAttributeOne.getAttributeValues().add(validFirstNameAttribute);

        Attribute validAttributeTwo = aSimpleStringAttribute().build();
        PersonName validSurnameAttributeValue = openSamlXmlObjectFactory.createPersonNameAttributeValue("Jones");
        validAttributeTwo.getAttributeValues().add(validSurnameAttributeValue);

        Attribute invalidMiddlenameAttribute = aSimpleStringAttribute().build();
        PersonName invalidMiddlenameAttributeValue = openSamlXmlObjectFactory.createPersonNameAttributeValue("Middle");
        invalidMiddlenameAttributeValue.setLanguage("en-US");
        invalidMiddlenameAttribute.getAttributeValues().add(invalidMiddlenameAttributeValue);

        final Assertion assertion = AssertionBuilder.anAssertion()
                .addAttributeStatement(anAttributeStatement().build())
                .addAttributeStatement(anAttributeStatement().build())
                .buildUnencrypted();

        assertion.getAttributeStatements().get(0).getAttributes().add(validAttributeOne);
        assertion.getAttributeStatements().get(1).getAttributes().addAll(asList(validAttributeTwo, invalidMiddlenameAttribute));

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> validator.validate(assertion),
                invalidAttributeLanguageInAssertion(invalidMiddlenameAttribute.getName(), invalidMiddlenameAttributeValue.getLanguage())
        );
    }

    @Test
    public void validate_shouldThrowWhenFraudEventNotCorrect() throws Exception{
        final Assertion assertion = AssertionBuilder.anAssertion()
                .addAttributeStatement(anAttributeStatement().addAttribute(IdpFraudEventIdAttributeBuilder.anIdpFraudEventIdAttribute().buildInvalidAttribute()).build())
                .buildUnencrypted();

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> validator.validateFraudEvent(assertion),
                invalidFraudAttribute(AssertionAttributeStatementValidator.INVALID_FRAUD_EVENT_TYPE)
        );

    }

}
