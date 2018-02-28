package uk.gov.ida.saml.core.validators.assertion;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import uk.gov.ida.saml.core.DateTimeFreezer;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.authnStatementAlreadyReceived;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.duplicateMatchingDataset;
import static uk.gov.ida.saml.core.test.SamlTransformationErrorManagerTestHelper.validateFail;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;
import static uk.gov.ida.saml.core.test.builders.SubjectBuilder.aSubject;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder.aSubjectConfirmation;
import static uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder.aSubjectConfirmationData;

@RunWith(OpenSAMLRunner.class)
public class DuplicateAssertionValidatorTest {

    private ConcurrentMap<String, DateTime> duplicateIds;
    private DuplicateAssertionValidator duplicateAssertionValidator;

    @Before
    public void setUp() {
        DateTimeFreezer.freezeTime();

        duplicateIds = new ConcurrentHashMap<>();
        duplicateIds.put("duplicate", DateTime.now().plusMinutes(5));
        duplicateIds.put("expired-duplicate", DateTime.now().minusMinutes(2));

        duplicateAssertionValidator = new DuplicateAssertionValidator(duplicateIds);
    }

    @Test
    public void validateAuthnStatementAssertion_shouldPassIfTheAssertionIsNotADuplicateOfAPreviousOne() throws Exception {
        Assertion assertion = anAssertion().withId("not-duplicate").buildUnencrypted();
        duplicateAssertionValidator.validateAuthnStatementAssertion(assertion);
    }

    @Test
    public void validateAuthnStatementAssertion_shouldPassIfTwoAssertionsHaveTheSameIdButTheFirstAssertionHasExpired() throws Exception {
        DateTime futureDate = DateTime.now().plusMinutes(6);

        Assertion assertion = createAssertion("expired-duplicate", futureDate);
        duplicateAssertionValidator.validateAuthnStatementAssertion(assertion);

        assertThat(duplicateIds.get("expired-duplicate")).isEqualTo(futureDate.toDateTime(UTC));
    }

    @Test
    public void validateAuthnStatementAssertion_shouldThrowAnExceptionIfTheAssertionIsADuplicateOfAPreviousOne() throws Exception {
        Assertion assertion = anAssertion().withId("duplicate").buildUnencrypted();
        validateFail(
            ()-> duplicateAssertionValidator.validateAuthnStatementAssertion(assertion),
            authnStatementAlreadyReceived("duplicate")
        );
    }

    @Test
    public void validateAuthnStatementAssertion_shouldStoreTheAssertionIdIfNotADuplicate() throws Exception {
        DateTime futureDate = DateTime.now().plusMinutes(6);

        Assertion assertion = createAssertion("new-id", futureDate);
        duplicateAssertionValidator.validateAuthnStatementAssertion(assertion);

        assertThat(duplicateIds.get("new-id")).isEqualTo(futureDate.toDateTime(UTC));
    }

    @Test
    public void validateMatchingDataSetAssertion_shouldPassIfTheAssertionIsNotADuplicateOfAPreviousOne() throws Exception {
        Assertion assertion = anAssertion().withId("not-duplicate").buildUnencrypted();
        duplicateAssertionValidator.validateMatchingDataSetAssertion(assertion, "issuer");
    }

    @Test
    public void validateMatchingDataSetAssertion_shouldPassIfTwoAssertionsHaveTheSameIdButTheFirstAssertionHasExpired() throws Exception {
        DateTime futureDate = DateTime.now().plusMinutes(6);

        Assertion assertion = createAssertion("expired-duplicate", futureDate);
        duplicateAssertionValidator.validateMatchingDataSetAssertion(assertion, "issuer");

        assertThat(duplicateIds.get("expired-duplicate")).isEqualTo(futureDate.toDateTime(UTC));
    }

    @Test
    public void validateMatchingDataSetAssertion_shouldThrowAnExceptionIfTheAssertionIsADuplicateOfAPreviousOne() throws Exception {
        Assertion assertion = anAssertion().withId("duplicate").buildUnencrypted();
        validateFail(
            ()-> duplicateAssertionValidator.validateMatchingDataSetAssertion(assertion, "issuer"),
            duplicateMatchingDataset("duplicate", "issuer")
        );
    }

    @Test
    public void validateMatchingDataSetAssertion_shouldStoreTheAssertionIdIfNotADuplicate() throws Exception {
        DateTime futureDate = DateTime.now().plusMinutes(6);

        Assertion assertion = createAssertion("new-id", futureDate);
        duplicateAssertionValidator.validateMatchingDataSetAssertion(assertion, "issuer");

        assertThat(duplicateIds.get("new-id")).isEqualTo(futureDate.toDateTime(UTC));
    }

    private Assertion createAssertion(String id, DateTime notOnOrAfter) {
        SubjectConfirmationData subjectConfirmationData = aSubjectConfirmationData()
            .withNotOnOrAfter(notOnOrAfter).build();
        SubjectConfirmation subjectConfirmation = aSubjectConfirmation()
            .withSubjectConfirmationData(subjectConfirmationData).build();
        Subject subject = aSubject()
            .withSubjectConfirmation(subjectConfirmation).build();
        return anAssertion()
            .withId(id)
            .withSubject(subject)
            .buildUnencrypted();
    }
}
