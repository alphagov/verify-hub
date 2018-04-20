package uk.gov.ida.saml.core.validators.assertion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import uk.gov.ida.saml.core.test.OpenSAMLMockitoRunner;
import uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper;
import uk.gov.ida.saml.core.validation.SamlValidationSpecificationFailure;
import uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory;import uk.gov.ida.saml.core.validators.subject.AssertionSubjectValidator;
import uk.gov.ida.saml.core.validators.subjectconfirmation.BasicAssertionSubjectConfirmationValidator;
import uk.gov.ida.saml.security.validators.issuer.IssuerValidator;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;

@RunWith(OpenSAMLMockitoRunner.class)
public class AssertionValidatorTest {

    @Mock
    private AssertionSubjectValidator subjectValidator;
    @Mock
    private IssuerValidator issuerValidator;
    @Mock
    private AssertionAttributeStatementValidator assertionAttributeStatementValidator;
    @Mock
    private BasicAssertionSubjectConfirmationValidator basicAssertionSubjectConfirmationValidator;

    private AssertionValidator validator;

    @Before
    public void setup() {
        validator = new AssertionValidator(issuerValidator, subjectValidator, assertionAttributeStatementValidator, basicAssertionSubjectConfirmationValidator);
    }

    @Test
    public void validate_shouldDelegateSubjectValidation() throws Exception {
        String requestId = UUID.randomUUID().toString();
        Assertion assertion = anAssertion()
                .withSubject(aSubject().build())
                .buildUnencrypted();

        validator.validate(assertion, requestId, "");

        verify(subjectValidator).validate(assertion.getSubject(), assertion.getID());
    }

    @Test
    public void validate_shouldDelegateSubjectConfirmationValidation() throws Exception {
        String requestId = UUID.randomUUID().toString();
        SubjectConfirmation subjectConfirmation = aSubjectConfirmation().build();
        Assertion assertion = anAssertion()
                .withSubject(aSubject().withSubjectConfirmation(subjectConfirmation).build())
                .buildUnencrypted();

        validator.validate(assertion, requestId, "");

        verify(basicAssertionSubjectConfirmationValidator).validate(subjectConfirmation);
    }

    @Test
    public void validate_shouldDelegateAttributeValidation() throws Exception {
        String requestId = UUID.randomUUID().toString();
        Assertion assertion = anAssertion()
                .withSubject(aSubject().build())
                .buildUnencrypted();

        validator.validate(assertion, requestId, "");

        verify(assertionAttributeStatementValidator).validate(assertion);
    }

    @Test
    public void validate_shouldThrowExceptionIfAnyAssertionDoesNotContainASignature() throws Exception {
        String someID = UUID.randomUUID().toString();
        Assertion assertion = anAssertion().withSignature(null).withId(someID).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.assertionSignatureMissing(someID));
    }

    @Test
    public void validate_shouldThrowExceptionIfAnAssertionIsNotSigned() throws Exception {
        String someID = UUID.randomUUID().toString();

        Assertion assertion = anAssertion().withoutSigning().withId(someID).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.assertionNotSigned(someID));
    }

    @Test
    public void validate_shouldDoNothingIfAllAssertionsAreSigned() throws Exception {
        Assertion assertion = anAssertion().buildUnencrypted();

        validator.validate(assertion, "", assertion.getID());
    }

    @Test
    public void validate_shouldThrowExceptionIfIdIsMissing() throws Exception {
        Assertion assertion = anAssertion().withId(null).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.missingId());
    }

    @Test
    public void validate_shouldThrowExceptionIfVersionIsMissing() throws Exception {
        Assertion assertion = anAssertion().withVersion(null).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.missingVersion(assertion.getID()));
    }

    @Test
    public void validate_shouldThrowExceptionIfVersionIsNotSamlTwoPointZero() throws Exception {
        Assertion assertion = anAssertion().withVersion(SAMLVersion.VERSION_10).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.illegalVersion(assertion.getID()));
    }

    @Test
    public void validate_shouldThrowExceptionIfIssueInstantIsMissing() throws Exception {
        Assertion assertion = anAssertion().withIssueInstant(null).buildUnencrypted();

        assertExceptionMessage(assertion, SamlTransformationErrorFactory.missingIssueInstant(assertion.getID()));
    }

    private void assertExceptionMessage(
            final Assertion assertion,
            SamlValidationSpecificationFailure failure) {

        SamlTransformationErrorManagerTestHelper.validateFail(
                new SamlTransformationErrorManagerTestHelper.Action() {
                    @Override
                    public void execute() {
                        validator.validate(assertion, "", "");
                    }
                },
                failure
        );
    }
}
