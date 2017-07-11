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
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.SimpleStringAttributeBuilder.aSimpleStringAttribute;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.aMatchingDatasetAttributeStatement_1_1;
import static uk.gov.ida.saml.core.test.builders.MatchingDatasetAttributeStatementBuilder_1_1.anEmptyMatchingDatasetAttributeStatement_1_1;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeBuilder_1_1.aPersonName_1_1;
import static uk.gov.ida.saml.core.test.builders.PersonNameAttributeValueBuilder.aPersonNameValue;

@RunWith(OpenSAMLMockitoRunner.class)
public class MatchingDatasetAssertionValidatorTest {

    private static final String RESPONSE_ISSUER_ID = "issuer ID";

    @Mock
    private DuplicateAssertionValidator duplicateAssertionValidator;

    private MatchingDatasetAssertionValidator validator;

    @Before
    public void setUp() {
        validator = new MatchingDatasetAssertionValidator(duplicateAssertionValidator);
        when(duplicateAssertionValidator.valid(any(Assertion.class))).thenReturn(true);
    }

    @Test
    public void validate_shouldThrowExceptionWhenNameIsNotRecognised() throws Exception {
        Attribute attribute = aSimpleStringAttribute().withName("dummy attribute").build();
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().addCustomAttribute(attribute).build();
        Assertion assertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.mdsAttributeNotRecognised("dummy attribute"));
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
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenSurNameIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withSurname(aPersonName_1_1().buildAsSurname()).build();
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenDateOfBirthIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withDateOfBirth().build();
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenGenderIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withGender().build();
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenCurrentAddressIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().withCurrentAddress().build();
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldNotThrowAnExceptionWhenPreviousAddressIsPresent_ProfileV1_1() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().addPreviousAddress().build();
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        validator.validate(matchingDatasetAssertion, RESPONSE_ISSUER_ID);
    }

    @Test
    public void validate_shouldThrowExceptionWhenNoAttributesArePresent() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().build();
        final Assertion matchingDatasetAssertion = anAssertion().addAttributeStatement(attributeStatement).buildUnencrypted();

        assertExceptionMessage(matchingDatasetAssertion, SamlTransformationErrorFactory.attributeStatementEmpty(matchingDatasetAssertion.getID()));
    }

    @Test
    public void validate_shouldThrowExceptionWhenNoAttributeStatementsArePresent() throws Exception {
        final Assertion matchingDatasetAssertion = anAssertion().buildUnencrypted();

        assertExceptionMessage(matchingDatasetAssertion,
                SamlTransformationErrorFactory.mdsStatementMissing());
    }

    @Test
    public void validate_shouldThrowExceptionWhenMultipleAttributeStatementsArePresent() throws Exception {
        AttributeStatement attributeStatement = anEmptyMatchingDatasetAttributeStatement_1_1().build();
        final Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        assertExceptionMessage(matchingDatasetAssertion,
                SamlTransformationErrorFactory.mdsMultipleStatements());
    }

    @Test
    public void validate_shouldThrowExceptionWhenAttributeIsMissingValue() throws Exception {
        final Attribute attribute = aPersonName_1_1()
                .buildAsFirstnameWithNoAttributeValues();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        final Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        assertExceptionMessage(matchingDatasetAssertion,
                SamlTransformationErrorFactory.emptyAttribute("MDS_firstname"));
    }

    @Test
    public void validate_shouldThrowExceptionWhenAttributeValueIsIncorrectType() throws Exception {
        final Attribute attribute = aSimpleStringAttribute().withName(IdaConstants.Attributes_1_1.Firstname.NAME).withSimpleStringValue("Joe").build();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        assertExceptionMessage(matchingDatasetAssertion,
                SamlTransformationErrorFactory.attributeWithIncorrectType(IdaConstants.Attributes_1_1.Firstname.NAME, PersonName.TYPE_NAME, StringBasedMdsAttributeValue.TYPE_NAME));
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

        assertExceptionMessage(matchingDatasetAssertion,
                SamlTransformationErrorFactory.attributeWithIncorrectType(IdaConstants.Attributes_1_1.DateOfBirth.NAME, Date.TYPE_NAME, PersonName.TYPE_NAME));
    }

    @Test
    public void validate_shouldThrowExceptionIfTheMatchingDatasetAssertionIdIsADuplicateOfPrevious() throws Exception {
        String assertionId = "some-id";
        Attribute attribute = aPersonName_1_1().addValue(aPersonNameValue().withTo(null).build()).buildAsFirstname();
        AttributeStatement attributeStatement = aMatchingDatasetAttributeStatement_1_1()
                .withFirstname(attribute)
                .build();
        Assertion matchingDatasetAssertion = anAssertion()
                .withId(assertionId)
                .addAttributeStatement(attributeStatement)
                .buildUnencrypted();

        when(duplicateAssertionValidator.valid(matchingDatasetAssertion)).thenReturn(false);
        assertExceptionMessage(matchingDatasetAssertion, SamlTransformationErrorFactory.duplicateMatchingDataset(assertionId, RESPONSE_ISSUER_ID));
    }

    private void assertExceptionMessage(
            final Assertion assertion,
            SamlValidationSpecificationFailure failure) {

        SamlTransformationErrorManagerTestHelper.validateFail(
                () -> validator.validate(assertion, RESPONSE_ISSUER_ID),
                failure
        );
    }
}
