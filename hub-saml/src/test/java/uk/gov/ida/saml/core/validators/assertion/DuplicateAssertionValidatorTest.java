package uk.gov.ida.saml.core.validators.assertion;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.core.DateTimeFreezer;
import uk.gov.ida.saml.core.test.OpenSAMLRunner;
import uk.gov.ida.saml.core.test.builders.SubjectBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationBuilder;
import uk.gov.ida.saml.core.test.builders.SubjectConfirmationDataBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ida.saml.core.test.builders.AssertionBuilder.anAssertion;

@RunWith(OpenSAMLRunner.class)
public class DuplicateAssertionValidatorTest {

    @Test
    public void valid_shouldThrowAnExceptionIfTheAssertionIsADuplicateOfAPreviousOne() throws Exception {
        DateTimeFreezer.freezeTime();
        String duplicateId = "duplicate";
        ConcurrentMap<String, DateTime> duplicateIds = new ConcurrentHashMap<>();
        duplicateIds.put(duplicateId, DateTime.now().plusMinutes(15));
        DuplicateAssertionValidator duplicateAssertionValidator = new DuplicateAssertionValidator(duplicateIds);

        Assertion assertion = anAssertion().withId(duplicateId).buildUnencrypted();
        boolean result = duplicateAssertionValidator.valid(assertion);

        assertThat(result).isEqualTo(false);
    }

    @Test
    public void valid_shouldPassIfTheAssertionIsNotADuplicateOfAPreviousOne() throws Exception {
        ConcurrentMap<String, DateTime> duplicateIds = new ConcurrentHashMap<>();
        duplicateIds.put("duplicate", DateTime.now().plusMinutes(5));

       DuplicateAssertionValidator duplicateAssertionValidator = new DuplicateAssertionValidator(duplicateIds);

        Assertion assertion = anAssertion().withId("not-duplicate").buildUnencrypted();
        boolean result = duplicateAssertionValidator.valid(assertion);

        assertThat(result).isEqualTo(true);
    }

    @Test
    public void valid_shouldStoreTheAssertionIdIfNotADuplicate() throws Exception {
        DateTimeFreezer.freezeTime();
        ConcurrentMap<String, DateTime> duplicateIds = new ConcurrentHashMap<>();
        DuplicateAssertionValidator duplicateAssertionValidator = new DuplicateAssertionValidator(duplicateIds);

        String id = "id-to-store";
        DateTime notOnOrAfter = DateTime.now().plusMinutes(6);
        Assertion assertion = anAssertion()
            .withId(id)
            .withSubject(SubjectBuilder.aSubject()
                .withSubjectConfirmation(SubjectConfirmationBuilder.aSubjectConfirmation()
                    .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                        .withNotOnOrAfter(notOnOrAfter)
                        .build())
                    .build()
                ).build()
            ).buildUnencrypted();
        duplicateAssertionValidator.valid(assertion);

        assertThat(duplicateIds.get(id)).isEqualTo(new DateTime(notOnOrAfter, DateTimeZone.UTC));
    }

    @Test
    public void valid_shouldPassIfTwoAssertionsHaveTheSameIdButTheFirstAssertionHasExpired() throws Exception {
        DateTimeFreezer.freezeTime();

        String duplicateId = "duplicate";
        ConcurrentMap<String, DateTime> duplicateIds = new ConcurrentHashMap<>();
        duplicateIds.put(duplicateId, DateTime.now().minusMinutes(2));

        DuplicateAssertionValidator duplicateAssertionValidator = new DuplicateAssertionValidator(duplicateIds);

        DateTime notOnOrAfter = DateTime.now().plusMinutes(6);
        Assertion assertion = anAssertion()
            .withId(duplicateId)
            .withSubject(SubjectBuilder.aSubject()
                .withSubjectConfirmation(SubjectConfirmationBuilder.aSubjectConfirmation()
                    .withSubjectConfirmationData(SubjectConfirmationDataBuilder.aSubjectConfirmationData()
                        .withNotOnOrAfter(notOnOrAfter)
                        .build())
                    .build()
                ).build()
            ).buildUnencrypted();
        boolean result = duplicateAssertionValidator.valid(assertion);

        assertThat(result).isEqualTo(true);
        assertThat(duplicateIds.get(duplicateId)).isEqualTo(new DateTime(notOnOrAfter, DateTimeZone.UTC));
    }
}
