package uk.gov.ida.saml.hub.validators.authnrequest;

import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import uk.gov.ida.saml.hub.configuration.SamlDuplicateRequestValidationConfiguration;

public class DuplicateAuthnRequestValidator {
    private final IdExpirationCache<AuthnRequestIdKey> previousRequests;
    private final Duration expirationDuration;

    @Inject
    public DuplicateAuthnRequestValidator(IdExpirationCache<AuthnRequestIdKey> previousRequests, SamlDuplicateRequestValidationConfiguration samlDuplicateRequestValidationConfiguration) {
        this.previousRequests = previousRequests;
        this.expirationDuration = Duration.millis(samlDuplicateRequestValidationConfiguration.getAuthnRequestIdExpirationDuration().toMilliseconds());
    }

    public boolean valid(String requestId) {
        AuthnRequestIdKey key = new AuthnRequestIdKey(requestId);

        if (previousRequests.contains(key) && previousRequests.getExpiration(key).isAfterNow()) {
            return false;
        }
        DateTime expire = DateTime.now().plus(expirationDuration);
        previousRequests.setExpiration(key, expire);
        return true;
    }
}
