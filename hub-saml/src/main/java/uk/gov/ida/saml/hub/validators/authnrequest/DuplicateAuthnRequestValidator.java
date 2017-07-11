package uk.gov.ida.saml.hub.validators.authnrequest;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;

import java.util.concurrent.ConcurrentMap;

public class DuplicateAuthnRequestValidator {
    private final ConcurrentMap<AuthnRequestIdKey, DateTime> previousRequestKeys;
    private final SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration;

    @Inject
    public DuplicateAuthnRequestValidator(ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds, SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration) {
        this.previousRequestKeys = duplicateIds;
        this.samlDuplicateRequestValidationConfiguration = samlDuplicateRequestValidationConfiguration;
    }

    public boolean valid(String requestId) {
        AuthnRequestIdKey key = new AuthnRequestIdKey(requestId);

        if (previousRequestKeys.containsKey(key) && previousRequestKeys.get(key).isAfter(DateTime.now())) {
            return false;
        }
        DateTime expire = DateTime.now().plus(samlDuplicateRequestValidationConfiguration.getAuthnRequestIdExpirationDuration().toMilliseconds());
        previousRequestKeys.put(key, expire);
        return true;
    }


}
