package uk.gov.ida.hub.samlengine.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import io.lettuce.core.codec.RedisCodec;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public abstract class ExpirationCacheRedisCodec<T> implements RedisCodec<T, DateTime> {
    private final ObjectMapper objectMapper;

    ExpirationCacheRedisCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DateTime decodeValue(ByteBuffer valueBytes) {
        try {
            InputStream inputStream = new ByteBufferBackedInputStream(valueBytes);
            return objectMapper.readValue(inputStream, DateTime.class);
        } catch (IOException e) {
            throw new RedisSerializationException("Error decoding expiration time", e);
        }
    }

    @Override
    public ByteBuffer encodeValue(DateTime expirationTime) {
        try {
            return ByteBuffer.wrap(objectMapper.writeValueAsBytes(expirationTime));
        } catch (JsonProcessingException e) {
            throw new RedisSerializationException("Error encoding expiration time", e);
        }
    }
}
