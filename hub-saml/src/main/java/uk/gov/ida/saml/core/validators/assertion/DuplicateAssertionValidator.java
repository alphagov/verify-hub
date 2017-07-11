package uk.gov.ida.saml.core.validators.assertion;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;

import java.util.concurrent.ConcurrentMap;

public class DuplicateAssertionValidator {

    private final ConcurrentMap<String, DateTime> duplicateIds;

    @Inject
    public DuplicateAssertionValidator(ConcurrentMap<String, DateTime> duplicateIds) {
        this.duplicateIds = duplicateIds;
    }

    public boolean valid(Assertion assertion) {
        if (duplicateIds.containsKey(assertion.getID()) && duplicateIds.get(assertion.getID()).isAfter(DateTime.now())) {
            return false;
        }
        DateTime expire = assertion.getSubject().getSubjectConfirmations().get(0).getSubjectConfirmationData().getNotOnOrAfter();
        duplicateIds.put(assertion.getID(), expire);
        return true;
    }
}
