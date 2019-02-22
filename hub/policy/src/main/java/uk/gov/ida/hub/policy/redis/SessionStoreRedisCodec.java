package uk.gov.ida.hub.policy.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import io.lettuce.core.codec.RedisCodec;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class SessionStoreRedisCodec implements RedisCodec<SessionId, State> {
    private final ObjectMapper objectMapper;

    public SessionStoreRedisCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SessionId decodeKey(ByteBuffer keyBytes) {
        try {
            InputStream inputStream = new ByteBufferBackedInputStream(keyBytes);
            return objectMapper.readValue(inputStream, SessionId.class);
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
        try {
            return ByteBuffer.wrap(objectMapper.writeValueAsBytes(sessionId));
        } catch (JsonProcessingException e) {
            throw new RedisSerializationException("Error encoding SessionId", e);
        }
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
