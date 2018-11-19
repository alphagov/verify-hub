package uk.gov.ida.saml.hub.validators.authnrequest;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;

import java.util.concurrent.ConcurrentMap;

public class DuplicateAuthnRequestValidator {
    private final ConcurrentMap<AuthnRequestIdKey, DateTime> previousRequestKeys;
    private final Duration expirationDuration;

    @Inject
    public DuplicateAuthnRequestValidator(ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds, SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration) {
        this(duplicateIds, Duration.millis(samlDuplicateRequestValidationConfiguration.getAuthnRequestIdExpirationDuration().toMilliseconds()));
    }

    public DuplicateAuthnRequestValidator(ConcurrentMap<AuthnRequestIdKey, DateTime> duplicateIds, Duration authnRequestIdExpirationDuration) {
        this.previousRequestKeys = duplicateIds;
        this.expirationDuration = authnRequestIdExpirationDuration;
    }

    public boolean valid(String requestId) {
        AuthnRequestIdKey key = new AuthnRequestIdKey(requestId);

        if (previousRequestKeys.containsKey(key) && previousRequestKeys.get(key).isAfter(DateTime.now())) {
            return false;
        }
        DateTime expire = DateTime.now().plus(expirationDuration);
        previousRequestKeys.put(key, expire);
        return true;
    }


}
