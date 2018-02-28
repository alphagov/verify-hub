package uk.gov.ida.saml.core.validators.assertion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import uk.gov.ida.saml.core.IdaConstants;
import uk.gov.ida.saml.core.extensions.Date;
import uk.gov.ida.saml.core.extensions.PersonName;
import uk.gov.ida.saml.core.extensions.StringBasedMdsAttributeValue;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.attributeStatementEmpty;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.attributeWithIncorrectType;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.emptyAttribute;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.mdsAttributeNotRecognised;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.mdsMultipleStatements;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.mdsStatementMissing;
import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.validateFail;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.anEmptyMatchingDatasetAttributeStatement_1_1;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeBuilder_1_1.aPersonName_1_1;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeValueBuilder.aPersonNameValue;
import static uk.gov.ida.saml.core.test.builders.SimpleStringAttributeBuilder.aSimpleStringAttribute;

@RunWith(OpenSAMLMockitoRunner.class)
public class MatchingDatasetAssertionValidatorTest {

    private static final String RESPONSE_ISSUER_ID = "issuer ID";

    @Mock
    private DuplicateAssertionValidator duplicateAssertionValidator;

    private MatchingDatasetAssertionValidator validator;

    @Before
    public void setUp() {
        validator = new MatchingDatasetAssertionValidator(duplicateAssertionValidator);
    }

    @Test
    public void validate_shouldThrowExceptionWhenNameIsNotRecognised() throws Exception {
        Attribute attribute = aSimpleStringAttribute().withName("dummy attribute").build();
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().addCustomAttribute(attribute).build();
        Assertion assertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validateFail(
            () -> validator.validate(assertion, RESPONSE_ISSUER_ID),
            mdsAttributeNotRecognised("dummy attribute")
        );
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenFirstnameIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withFirstname(aPersonName_1_1().buildAsFirstname()).build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenMiddleNameIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withMiddleNames(aPersonName_1_1().buildAsMiddlename()).build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenSurNameIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withSurname(aPersonName_1_1().buildAsSurname()).build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenDateOfBirthIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withDateOfBirth().build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenGenderIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withGender().build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenCurrentAddressIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withCurrentAddress().build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenPreviousAddressIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().addPreviousAddress().build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldThrowExceptionWhenNoAttributesArePresent() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validateFail(
            () -> validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID),
            attributeStatementEmpty(matchingDatasetAssertion.getID())
        );
    }

    @Test
    public void validate_shouldThrowExceptionWhenNoAttributeStatementsArePresent() throws Exception {
        Assertion matchingDatasetAssertion = anAssertion().buildUnencrypted();

        validateFail(
            () -> validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID),
            mdsStatementMissing()
        );
    }

    @Test
    public void validate_shouldThrowExceptionWhenMultipleAttributeStatementsArePresent() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validateFail(
            () -> validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID),
            mdsMultipleStatements()
        );
    }

    @Test
    public void validate_shouldThrowExceptionWhenAttributeIsMissingValue() throws Exception {
        Attribute attribute = aPersonName_1_1()
                .buildAsFirstnameWithNoAttributeValues();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validateFail(
            () -> validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID),
            emptyAttribute("MDS_firstname")
        );
    }

    @Test
    public void validate_shouldThrowExceptionWhenAttributeValueIsIncorrectType() throws Exception {
        Attribute attribute = aSimpleStringAttribute().withName(IdaConstants.Attributes_1_1.Firstname.NAME).withSimpleStringValue("Joe").build();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validateFail(
            () -> validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID),
            attributeWithIncorrectType(IdaConstants.Attributes_1_1.Firstname.NAME, PersonName.TYPE_NAME, StringBasedMdsAttributeValue.TYPE_NAME)
        );
    }

    @Test
    public void validate_shouldNotThrowExceptionWhenAttributeValueVerifiedIsAbsent() throws Exception {
        Attribute attribute = aPersonName_1_1().addValue(aPersonNameValue().withVerified(null).build()).buildAsFirstname();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowExceptionWhenAttributeValueToDateIsAbsent() throws Exception {
        Attribute attribute = aPersonName_1_1().addValue(aPersonNameValue().withTo(null).build()).buildAsFirstname();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowExceptionWhenAttributeValueFromDateIsAbsent() throws Exception {
        Attribute attribute = aPersonName_1_1().addValue(aPersonNameValue().withFrom(null).build()).buildAsFirstname();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldThrowExceptionWhenAttributeValueTypeIsValidButIncorrectForAttribute() throws Exception {
        Attribute attribute = aPersonName_1_1().addValue(aPersonNameValue().withFrom(null).build()).buildAsFirstname();
        attribute.setName(IdaConstants.Attributes_1_1.DateOfBirth.NAME);
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withDateOfBirth(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        validateFail(
            () -> validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID),
            attributeWithIncorrectType(IdaConstants.Attributes_1_1.DateOfBirth.NAME, Date.TYPE_NAME, PersonName.TYPE_NAME)
        );
    }

    @Test
    public void validate_shouldValidateForDuplicateIds() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withFirstname(aPersonName_1_1().buildAsFirstname()).build();
        Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();
        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
        verify(duplicateAssertionValidator, times(1)).validateMatchingDataSetAssertion(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }
}
