package uk.gov.ida.hub.policy.redis;

class RedisSerializationException extends RuntimeException {
    RedisSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
