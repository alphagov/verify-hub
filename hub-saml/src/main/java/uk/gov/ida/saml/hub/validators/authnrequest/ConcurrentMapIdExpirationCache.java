package uk.gov.ida.saml.hub.validators.authnrequest;

import org.joda.time.DateTime;

import java.util.concurrent.ConcurrentMap;

public class ConcurrentMapIdExpirationCache<T> implements IdExpirationCache<T> {
    private final ConcurrentMap<T, DateTime> infinispanMap;

    public ConcurrentMapIdExpirationCache(ConcurrentMap<T, DateTime> infinispanMap) {
        this.infinispanMap = infinispanMap;
    }

    @Override
    public boolean contains(T key) {
        return infinispanMap.containsKey(key);
    }

    @Override
    public DateTime getExpiration(T key) {
        return infinispanMap.get(key);
    }

    @Override
    public void setExpiration(T key, DateTime expirationTime) {
        infinispanMap.put(key, expirationTime);
    }
}
