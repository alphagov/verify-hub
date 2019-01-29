package uk.gov.ida.hub.policy.session;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.redisson.api.RMapCache;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.domain.state.SessionStartedState;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.verify;
import static uk.gov.ida.hub.policy.builder.domain.SessionIdBuilder.aSessionId;

@RunWith(MockitoJUnitRunner.class)
public class RedisSessionStoreTest {

    private static final Long EXPIRY_TIME = 1L;
    private RedisSessionStore redisSessionStore;

    @Mock
    private RMapCache<SessionId, State> redisMap;

    @Before
    public void setUp() {
        redisSessionStore = new RedisSessionStore(redisMap, EXPIRY_TIME);
    }

    @Test
    public void shouldInsertIntoRedisWithExpiry() {
        SessionId sessionId = aSessionId().build();
        State state = getRandomState();
        redisSessionStore.insert(sessionId, state);

        verify(redisMap).put(sessionId, state, EXPIRY_TIME, TimeUnit.MINUTES);
    }

    @Test
    public void shouldReplaceSessionInRedis() {
        SessionId sessionId = aSessionId().build();
        State state = getRandomState();
        redisSessionStore.replace(sessionId, state);

        verify(redisMap).replace(sessionId, state);
    }

    @Test
    public void shouldCheckIfSessionExists() {
        SessionId sessionId = aSessionId().build();
        redisSessionStore.hasSession(sessionId);

        verify(redisMap).containsKey(sessionId);
    }

    @Test
    public void shouldGetASessionFromRedis() {
        SessionId sessionId = aSessionId().build();
        redisSessionStore.get(sessionId);

        verify(redisMap).get(sessionId);
    }

    private State getRandomState() {
       return new SessionStartedState(
               UUID.randomUUID().toString(), "a-relay-state", "a-request-issuer", URI.create("/an-endpoint"), false,
               DateTime.now().plusMinutes(5), aSessionId().build(), false
       );
    }
}