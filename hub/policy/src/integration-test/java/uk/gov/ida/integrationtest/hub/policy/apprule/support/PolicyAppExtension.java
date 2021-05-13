package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import certificates.values.CACertificates;
import io.dropwizard.Application;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.lang3.ArrayUtils;
import redis.embedded.Redis;
import redis.embedded.RedisServer;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;
import uk.gov.ida.hub.policy.PolicyModule;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.redis.SessionStoreRedisCodec;
import uk.gov.ida.hub.policy.session.RedisSessionStore;

import java.time.Duration;

public class PolicyAppExtension extends TestDropwizardAppExtension {

    private static final int REDIS_PORT = 6381;
    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static Redis redis;

    public static PolicyBuilder forApp(final Class<? extends Application> app) {
        return new PolicyBuilder(app);
    }

    public static class PolicyBuilder extends TestDropwizardAppExtension.Builder {
        public PolicyBuilder(Class<? extends Application> app) {
            super(app);
            clientTrustStore.create();
            redis = RedisServer.builder().setting("bind 127.0.0.1").port(REDIS_PORT).build();
            redis.start();
        }

        public PolicyBuilder withDefaultConfigOverridesAnd(String... extraOverrides) {
            String[] defaultOverrides = {
                    "clientTrustStoreConfiguration.path: " + clientTrustStore.getAbsolutePath(),
                    "clientTrustStoreConfiguration.password: " + clientTrustStore.getPassword(),
                    "eventEmitterConfiguration.enabled: false",
                    "sessionStore.redis.uri: redis://localhost:" + REDIS_PORT
            };
            this.configOverrides(ArrayUtils.addAll(defaultOverrides, extraOverrides));
            return this;
        }

    }

    public static void tearDown() {
        clientTrustStore.delete();
        redis.stop();
    }

    public static <T extends State> T getSessionState(SessionId sessionId, Class<T> stateClazz) {
        StatefulRedisConnection<SessionId, State> redisConnection = RedisClient.create().connect(new SessionStoreRedisCodec(PolicyModule.getRedisObjectMapper()), new RedisURI("localhost", REDIS_PORT, Duration.ofSeconds(2)));
        RedisSessionStore redisSessionStore = new RedisSessionStore(redisConnection.sync(), 3600L);
        return stateClazz.cast(redisSessionStore.get(sessionId));
    }
}
