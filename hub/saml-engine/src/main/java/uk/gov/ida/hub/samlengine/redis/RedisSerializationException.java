package uk.gov.ida.hub.samlengine.redis;

class RedisSerializationException extends RuntimeException {
    RedisSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
