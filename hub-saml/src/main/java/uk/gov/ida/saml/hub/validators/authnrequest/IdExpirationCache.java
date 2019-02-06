package uk.gov.ida.saml.hub.validators.authnrequest;

import org.joda.time.DateTime;

public interface IdExpirationCache<T> {
    boolean contains(T key);

    DateTime getExpiration(T key);

    void setExpiration(T key, DateTime dateTime);
}
