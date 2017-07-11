package uk.gov.ida.saml.core.validators.subjectconfirmation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;

@RunWith(OpenSAMLMockitoRunner.class)
public class AssertionSubjectConfirmationValidatorTest {

    private static final String REQUEST_ID = "some-request-id";

    private AssertionSubjectConfirmationValidator validator;

    @Before
    public void setup() {
        validator = new AssertionSubjectConfirmationValidator();
    }

    @Test
    public void validate_shouldThrowExceptionIfSubjectConfirmationDataRecipientAttributeDoesNotMatchTheExpectedIssuerId() throws Exception {
        final String expectedRecipientId = TestEntityIds.HUB_ENTITY_ID;
        final String actualRecipientId = TestEntityIds.TEST_RP;
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
                .withSubjectConfirmationData(createSubjectConfirmationDataBuilder()
                        .withRecipient(actualRecipientId)
                        .build())
                .build();
        assertExceptionMessage(subjectConfirmation, SamlTransformationErrorFactory.incorrectRecipientFormat(actualRecipientId, expectedRecipientId), expectedRecipientId);
    }

    @Test
    public void validate_shouldThrowExceptionIfSubjectConfirmationDataInResponseToAttributeIsNotTheOriginalRequestId() throws Exception {
        final String subjectInResponseTo = "an-incorrect-request-id";
        final SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
                .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                        .withInResponseTo(subjectInResponseTo)
                        .build())
                .build();

        assertExceptionMessage(
                subjectConfirmation,
                SamlTransformationErrorFactory.notMatchInResponseTo(subjectInResponseTo, REQUEST_ID),
                subjectConfirmation.getSubjectConfirmationData().getRecipient());
    }

    private void assertExceptionMessage(
            final SubjectConfirmation subjectConfirmation,
            SamlValidationSpecificationFailure failure, final String recipient) {

        SamlTransformationErrorManagerTestHelper.validateFail(
                new SamlTransformationErrorManagerTestHelper.Action() {
                    @Override
                    public void execute() {
                        validator.validate(subjectConfirmation, REQUEST_ID, recipient);
                    }
                },
                failure
        );
    }

    private SubjectConfirmationDataBuilder createSubjectConfirmationDataBuilder() {
        return SubjectConfirmationDataBuilder.aSubjectConfirmationData().withInResponseTo(REQUEST_ID);
    }
}
