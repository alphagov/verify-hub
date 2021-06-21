package uk.gov.ida.saml.core.validators.assertion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.IPAddressAttributeBuilder;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.AttributeStatementBuilder.anAttributeStatement;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeBuilder_1_1.aPersonName_1_1;

@RunWith(OpenSAMLMockitoRunner.class)
public class IPAddressValidatorTest {

    private IPAddressValidator validator;

    @Before
    public void setUp() throws Exception {
        validator = new IPAddressValidator();
    }

    @Test
    public void validate_shouldThrowWhenAssertionDoesNotContainAnAttributeStatement() throws Exception {
        Assertion assertion = anAssertion().buildUnencrypted();
        validateException(SamlTransformationErrorFactory.missingIPAddress(assertion.getID()), assertion);
    }

    @Test
    public void validate_shouldNotThrowWhenFirstAttributeStatementContainsAnIPAddressAttribute() throws Exception {
        Assertion assertion = anAssertion()
                .addAttributeStatement(anAttributeStatement().addAttribute(IPAddressAttributeBuilder.anIPAddress().build()).build())
                .buildUnencrypted();

        validator.validate(assertion);
    }

    @Test
    public void validate_shouldNotThrowWhenSecondAttributeStatementContainsAnIPAddressAttribute() throws Exception {
        Assertion assertion = anAssertion()
                .addAttributeStatement(anAttributeStatement().build())
                .addAttributeStatement(anAttributeStatement().addAttribute(IPAddressAttributeBuilder.anIPAddress().build()).build())
                .buildUnencrypted();

        validator.validate(assertion);
    }

    @Test
    public void validate_shouldNotThrowWhenFirstAttributeStatementContainsMultipleAttributesIncludingIPAddressAttribute() throws Exception {
        Assertion assertion = anAssertion()
                .addAttributeStatement(anAttributeStatement()
                        .addAttribute(aPersonName_1_1().buildAsFirstname())
                        .addAttribute(IPAddressAttributeBuilder.anIPAddress().build())
                        .build())
                .buildUnencrypted();

        validator.validate(assertion);
    }

    @Test
    public void validate_shouldThrowWhenAssertionContainsAttributeStatementsButNoIPAddressAttribute() throws Exception {
        Assertion assertion = anAssertion()
                .addAttributeStatement(anAttributeStatement().build())
                .buildUnencrypted();
        validateException(SamlTransformationErrorFactory.missingIPAddress(assertion.getID()), assertion);
    }

    @Test
    public void validate_shouldThrowWhenAssertionContainsIPAddressAttributeWithNoValue() throws Exception {
        Assertion assertion = anAssertion()
                .addAttributeStatement(anAttributeStatement().addAttribute(IPAddressAttributeBuilder.anIPAddress().withValue(null).build()).build())
                .buildUnencrypted();

        validateException(SamlTransformationErrorFactory.emptyIPAddress(assertion.getID()), assertion);
    }

    @Test
    public void validate_shouldNotWarnWhenAssertionContainsAnInvalidIPAddress() throws Exception {
        final String ipAddress = "10.10.10.1a";
        final Assertion assertion = anAssertion()
                .addAttributeStatement(anAttributeStatement().addAttribute(IPAddressAttributeBuilder.anIPAddress().withValue(ipAddress).build()).build())
                .buildUnencrypted();

        validator.validate(assertion);
    }

    private void validateException(SamlValidationSpecificationFailure failure, final Assertion assertion) {
        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> validator.validate(assertion),
                failure
        );
    }
}
