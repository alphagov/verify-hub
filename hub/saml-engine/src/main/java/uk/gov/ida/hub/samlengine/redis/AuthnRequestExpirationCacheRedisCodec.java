package uk.gov.ida.hub.samlengine.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.google.common.io.ByteStreams;
import uk.gov.ida.saml.hub.validators.authnrequest.AuthnRequestIdKey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;

public class AuthnRequestExpirationCacheRedisCodec extends ExpirationCacheRedisCodec<AuthnRequestIdKey> {

    public AuthnRequestExpirationCacheRedisCodec(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public AuthnRequestIdKey decodeKey(ByteBuffer keyBytes) {
        try {
            InputStream inputStream = new ByteBufferBackedInputStream(keyBytes);
            // TODO: Replace with `inputStream.readAllBytes()` when we only use Java 9+
            return new AuthnRequestIdKey(new String(ByteStreams.toByteArray(inputStream), UTF_8));
        } catch (IOException e) {
            throw new RedisSerializationException("Error decoding key", e);
        }
    }

    @Override
    public ByteBuffer encodeKey(AuthnRequestIdKey key) {
        return ByteBuffer.wrap(key.getRequestId().getBytes());
    }
}
