package uk.gov.ida.hub.policy.exception;

import uk.gov.ida.hub.policy.domain.SessionId;

import static java.text.MessageFormat.format;

public class EidasCountryNotSupportedException extends RuntimeException {
    public EidasCountryNotSupportedException(SessionId sessionId, String countryCode) {
        super(format("The given country {0} is not supported. Session Id: {1}",
            countryCode, sessionId));
    }
}
