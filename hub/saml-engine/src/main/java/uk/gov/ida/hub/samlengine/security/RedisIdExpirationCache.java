package uk.gov.ida.hub.samlengine.security;

import io.lettuce.core.api.sync.RedisCommands;
import org.joda.time.DateTime;
import uk.gov.ida.saml.hub.validators.authnrequest.IdExpirationCache;

public class RedisIdExpirationCache<T> implements IdExpirationCache<T> {
    private final RedisCommands<T, DateTime> redis;
    private final Long recordTTL;

    public RedisIdExpirationCache(RedisCommands<T, DateTime> redis,
                                  Long recordTTL) {
        this.redis = redis;
        this.recordTTL = recordTTL;
    }

    @Override
    public boolean contains(T key) {
        return redis.exists(key) > 0;
    }

    @Override
    public DateTime getExpiration(T key) {
        return redis.get(key);
    }

    @Override
    public void setExpiration(T key, DateTime expirationTime) {
        redis.setex(key, recordTTL, expirationTime);
    }
}
