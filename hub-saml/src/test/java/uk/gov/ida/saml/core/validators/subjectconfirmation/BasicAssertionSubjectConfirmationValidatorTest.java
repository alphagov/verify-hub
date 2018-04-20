package uk.gov.ida.saml.core.validators.subjectconfirmation;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.core.DateTimeFreezer;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;

@RunWith(OpenSAMLMockitoRunner.class)
public class BasicAssertionSubjectConfirmationValidatorTest {

    private static final String REQUEST_ID = "some-request-id";

    private BasicAssertionSubjectConfirmationValidator validator;

    @Before
    public void setup() {
        validator = new BasicAssertionSubjectConfirmationValidator();
    }

    @After
    public void teardown() {
        DateTimeFreezer.unfreezeTime();
    }

    @Test
    public void validate_shouldThrowExceptionWhenSubjectConfirmationDataElementIsMissing() throws Exception {
        SubjectConfirmation subjectConfirmation = aSubjectConfirmation().withSubjectConfirmationData(null).build();
        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.missingSubjectConfirmationData());
    }

    @Test
    public void validate_shouldThrowExceptionWhenSubjectConfirmationDataRecipientAttributeIsMissing() throws Exception {
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation().withSubjectConfirmationData(createSubjectConfirmationDataBuilder().withRecipient(null).build()).build();
        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.missingBearerRecipient());
    }

    @Test
    public void validate_shouldThrowExceptionWhenSubjectConfirmationDataNotOnOrAfterAttributeIsMissing() throws Exception {
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
                .withSubjectConfirmationData(createSubjectConfirmationDataBuilder().withNotOnOrAfter(null).build())
                .build();

        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.missingNotOnOrAfter());
    }

    @Test
    public void validate_shouldThrowExceptionWhenSubjectConfirmationDataNotOnOrAfterIsNow() throws Exception {
        DateTimeFreezer.freezeTime();
        DateTime expiredTime = DateTime.now(DateTimeZone.UTC);
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
                .withSubjectConfirmationData(createSubjectConfirmationDataBuilder().withNotOnOrAfter(expiredTime).build())
                .build();

        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.exceededNotOnOrAfter(expiredTime));
    }

    @Test
    public void validate_shouldThrowExceptionWhenSubjectConfirmationDataNotOnOrAfterHasBeenExceeded() throws Exception {
        DateTimeFreezer.freezeTime();
        DateTime expiredTime = DateTime.now(DateTimeZone.UTC).minus(1);
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
                .withSubjectConfirmationData(createSubjectConfirmationDataBuilder().withNotOnOrAfter(expiredTime).build())
                .build();

        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.exceededNotOnOrAfter(expiredTime));
    }

    @Test
    public void validate_shouldThrowExceptionWhenSubjectConfirmationDataNotBeforeAttributeIsSet() throws Exception {
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
                .withSubjectConfirmationData(createSubjectConfirmationDataBuilder().withNotBefore(DateTime.now()).build())
                .build();
        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.notBeforeExists());
    }

    @Test
    public void validate_shouldDoNothingIfSubjectConfirmationDataHasAnAddressElement() throws Exception {
        final SubjectConfirmationData subjectConfirmationData = createSubjectConfirmationDataBuilder().withAddress("address").build();
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation().withSubjectConfirmationData(subjectConfirmationData).build();
        validator.validate(subjectConfirmation);
    }

    @Test
    public void validate_shouldThrowExceptionIfSubjectConfirmationDataInResponseToAttributeIsMissing() throws Exception {
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation().withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData().withInResponseTo(null).build()).build();
        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.missingBearerInResponseTo());
    }

    private void assertExceptionMessage(
            final SubjectConfirmation subjectConfirmation,
            SamlValidationSpecificationFailure failure) {

        SamlTransformationErrorManagerTestHelper.validateFail(
                new SamlTransformationErrorManagerTestHelper.Action() {
                    @Override
                    public void execute() {
                        validator.validate(subjectConfirmation);
                    }
                },
                failure
        );
    }

    private SubjectConfirmationDataBuilder createSubjectConfirmationDataBuilder() {
        return SubjectConfirmationDataBuilder.aSubjectConfirmationData().withInResponseTo(REQUEST_ID);
    }
}
