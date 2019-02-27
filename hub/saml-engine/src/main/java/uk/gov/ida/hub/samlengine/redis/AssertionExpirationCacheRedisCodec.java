package uk.gov.ida.hub.samlengine.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AssertionExpirationCacheRedisCodec extends ExpirationCacheRedisCodec<String> {

    public AssertionExpirationCacheRedisCodec(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public String decodeKey(ByteBuffer keyBytes) {
        try {
            InputStream inputStream = new ByteBufferBackedInputStream(keyBytes);
            // TODO: Replace with `inputStream.readAllBytes()` when we only use Java 9+
            return new String(ByteStreams.toByteArray(inputStream), UTF_8);
        } catch (IOException e) {
            throw new RedisSerializationException("Error decoding key", e);
        }
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return ByteBuffer.wrap(key.getBytes());
    }
}
