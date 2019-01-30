package uk.gov.ida.hub.samlengine.security;

import org.joda.time.DateTime;
import org.redisson.api.RMapCache;
import uk.gov.ida.saml.hub.validators.authnrequest.IdExpirationCache;

import java.util.concurrent.TimeUnit;

public class RedisIdExpirationCache<T> implements IdExpirationCache<T> {
    private final RMapCache<T, DateTime> redisMapCache;
    private final Long expiryTimeInMinutes;

    public RedisIdExpirationCache(RMapCache<T, DateTime> redisMapCache,
                                  Long expiryTimeInMinutes) {
        this.redisMapCache = redisMapCache;
        this.expiryTimeInMinutes = expiryTimeInMinutes;
    }

    @Override
    public boolean contains(T key) {
        return redisMapCache.containsKey(key);
    }

    @Override
    public DateTime getExpiration(T key) {
        return redisMapCache.get(key);
    }

    @Override
    public void setExpiration(T key, DateTime expirationTime) {
        redisMapCache.put(key, expirationTime, expiryTimeInMinutes, TimeUnit.MINUTES);
    }
}
