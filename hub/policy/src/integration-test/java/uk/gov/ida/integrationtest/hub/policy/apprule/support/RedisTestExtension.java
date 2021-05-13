package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import redis.embedded.Redis;
import redis.embedded.RedisServer;

import java.io.IOException;

public class RedisTestExtension implements BeforeAllCallback, AfterAllCallback {
    private Redis redis;

    public RedisTestExtension(int port) {
        redis = RedisServer.builder().setting("bind 127.0.0.1").port(port).build();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        redis.stop();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        redis.start();
    }
}
