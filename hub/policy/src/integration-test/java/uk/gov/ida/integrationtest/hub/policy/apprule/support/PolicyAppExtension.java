package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import certificates.values.CACertificates;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import keystore.KeyStoreResource;
import keystore.builders.KeyStoreResourceBuilder;
import org.apache.commons.lang3.ArrayUtils;
import redis.embedded.Redis;
import redis.embedded.RedisServer;
import uk.gov.ida.hub.policy.PolicyModule;
import uk.gov.ida.hub.policy.domain.SessionId;
import uk.gov.ida.hub.policy.domain.State;
import uk.gov.ida.hub.policy.redis.SessionStoreRedisCodec;
import uk.gov.ida.hub.policy.session.RedisSessionStore;

import javax.annotation.Nullable;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.time.Duration;

import static io.dropwizard.testing.ConfigOverride.config;

public class PolicyAppExtension extends DropwizardAppExtension {

    private static final int REDIS_PORT = 6381;
    private static final KeyStoreResource clientTrustStore = KeyStoreResourceBuilder.aKeyStoreResource().withCertificate("interCA", CACertificates.TEST_CORE_CA).withCertificate("rootCA", CACertificates.TEST_ROOT_CA).withCertificate("idpCA", CACertificates.TEST_IDP_CA).build();
    private static Redis redis;

    public PolicyAppExtension(Class applicationClass, @Nullable String configPath, ConfigOverride... configOverrides) {
        super(applicationClass, configPath, configOverrides);
    }

    public static <T extends State> T getSessionState(SessionId sessionId, Class<T> stateClazz) {
        StatefulRedisConnection<SessionId, State> redisConnection = RedisClient.create().connect(new SessionStoreRedisCodec(PolicyModule.getRedisObjectMapper()), new RedisURI("localhost", REDIS_PORT, Duration.ofSeconds(2)));
        RedisSessionStore redisSessionStore = new RedisSessionStore(redisConnection.sync(), 3600L);
        return stateClazz.cast(redisSessionStore.get(sessionId));
    }

    public static void tearDown() {
        clientTrustStore.delete();
        redis.stop();
    }

    public static PolicyAppExtensionBuilder builder() { return new PolicyAppExtensionBuilder(); }

    public PolicyClient getClient() {
        return new PolicyClient();
    }

    public static class PolicyAppExtensionBuilder {
        private ConfigOverride[] configOverrides = new ConfigOverride[]{};

        public PolicyAppExtensionBuilder withConfigOverrides(ConfigOverride... overrides) {
            configOverrides = overrides;
            return this;
        }

        public PolicyAppExtension build() {
            clientTrustStore.create();
            redis = RedisServer.builder().setting("bind 127.0.0.1").port(REDIS_PORT).build();
            redis.start();

            return new PolicyAppExtension(
                    PolicyIntegrationApplication.class,
                    ResourceHelpers.resourceFilePath("policy.yml"),
                    ArrayUtils.addAll(configOverrides, defaultConfigOverrides())
            );
        }

        private static ConfigOverride[] defaultConfigOverrides() {
            return new ConfigOverride[]{
                    config("clientTrustStoreConfiguration.path", clientTrustStore.getAbsolutePath()),
                    config("clientTrustStoreConfiguration.password", clientTrustStore.getPassword()),
                    config("eventEmitterConfiguration.enabled", "false"),
                    config("sessionStore.redis.uri", "redis://localhost:" + REDIS_PORT)
            };
        }
    }

    public class PolicyClient {
        private Client client;

        public PolicyClient() { client = client(); }

        public Response getTargetMain(URI uri) { return getTarget(uri, getLocalPort()); }

        public Response postTargetMain(URI uri, Object entity) { return postTarget(uri, getLocalPort(), entity); };

        public Response postTargetMain(String path, Object entity) { return postTargetMain(UriBuilder.fromPath(path).build(), entity); }

        public <T> T postTargetMain(String path, Object entity, Class<T> responseClass) { return postTarget(UriBuilder.fromPath(path).build(), getLocalPort(), entity, responseClass); };

        public Response postTarget(URI uri, int port, Object entity) {
            return client.target(buildUri(uri, port))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        }

        public <T> T postTarget(URI uri, int port, Object entity, Class<T> responseClass) {
            return client.target(buildUri(uri, port))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(
                            Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE),
                            responseClass
                    );
        }

        public Response getTarget(URI uri, int port) {
            return client.target(buildUri(uri, port))
                    .request()
                    .get();
        }

        private URI buildUri(URI baseUri, int port) {
            UriBuilder uriBuilder = UriBuilder.fromUri("http://localhost").port(port).path(baseUri.getRawPath());
            if (baseUri.getQuery() != null) {
                uriBuilder.replaceQuery(baseUri.getQuery());
            }

            return uriBuilder.build();
        }
    }
}
