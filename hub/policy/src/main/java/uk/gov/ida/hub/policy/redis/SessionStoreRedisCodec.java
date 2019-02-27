package uk.gov.ida.hub.policy.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.io.ByteStreams;
import io.lettuce.core.codec.RedisCodec;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SessionStoreRedisCodec implements RedisCodec<SessionId, State> {
    private final ObjectMapper objectMapper;

    public SessionStoreRedisCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SessionId decodeKey(ByteBuffer keyBytes) {
        try {
            InputStream inputStream = new ByteBufferBackedInputStream(keyBytes);
            // TODO: Replace with `inputStream.readAllBytes()` when we only use Java 9+
            return new SessionId(new String(ByteStreams.toByteArray(inputStream), UTF_8));
        } catch (IOException e) {
            throw new RedisSerializationException("Error decoding SessionId", e);
        }
    }

    @Override
    public State decodeValue(ByteBuffer valueBytes) {
        try {
            InputStream inputStream = new ByteBufferBackedInputStream(valueBytes);
            return objectMapper.readValue(inputStream, State.class);
        } catch (IOException e) {
            throw new RedisSerializationException("Error decoding State", e);
        }
    }

    @Override
    public ByteBuffer encodeKey(SessionId sessionId) {
        return ByteBuffer.wrap(sessionId.getSessionId().getBytes());
    }

    @Override
    public ByteBuffer encodeValue(State state) {
        try {
            return ByteBuffer.wrap(objectMapper.writeValueAsBytes(state));
        } catch (JsonProcessingException e) {
            throw new RedisSerializationException("Error encoding State", e);
        }
    }
}
