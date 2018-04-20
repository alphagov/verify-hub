package uk.gov.ida.saml.core.validators.assertion;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import uk.gov.ida.saml.hub.exception.SamlValidationException;

import java.util.concurrent.ConcurrentMap;

import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.authnStatementAlreadyReceived;
import static uk.gov.ida.saml.core.errors.SamlTransformationErrorFactory.duplicateMatchingDataset;

public class DuplicateAssertionValidator {

    private final ConcurrentMap<String, DateTime> duplicateIds;

    @Inject
    public DuplicateAssertionValidator(ConcurrentMap<String, DateTime> duplicateIds) {
        this.duplicateIds = duplicateIds;
    }

    public void validateAuthnStatementAssertion(Assertion assertion) {
        if (!valid(assertion)) throw new SamlValidationException(authnStatementAlreadyReceived(assertion.getID()));
    }

    public void validateMatchingDataSetAssertion(Assertion assertion, String responseIssuerId) {
        if (!valid(assertion)) throw new SamlValidationException(duplicateMatchingDataset(assertion.getID(), responseIssuerId));
    }

    private boolean valid(Assertion assertion) {
        if (isDuplicateNonExpired(assertion)) return false;

        DateTime expire = assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter();
        duplicateIds.put(assertion.getID(), expire);
        return true;
    }

    private boolean isDuplicateNonExpired(Assertion assertion) {
        return duplicateIds.containsKey(assertion.getID()) && duplicateIds.get(assertion.getID()).isAfter(DateTime.now());
    }
}
