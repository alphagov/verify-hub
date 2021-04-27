package uk.gov.ida.saml.hub.validators.authnrequest;

import org.joda.time.DateTime;

import java.util.concurrent.ConcurrentMap;

public class ConcurrentMapIdExpirationCache<T> implements IdExpirationCache<T> {
    private final ConcurrentMap<T, DateTime> map;

    public ConcurrentMapIdExpirationCache(ConcurrentMap<T, DateTime> map) {
        this.map = map;
    }

    @Override
    public boolean contains(T key) {
        return map.containsKey(key);
    }

    @Override
    public DateTime getExpiration(T key) {
        return map.get(key);
    }

    @Override
    public void setExpiration(T key, DateTime expirationTime) {
        map.put(key, expirationTime);
    }
}
