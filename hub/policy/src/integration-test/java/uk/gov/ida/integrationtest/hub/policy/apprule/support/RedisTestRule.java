package uk.gov.ida.integrationtest.hub.policy.apprule.support;

import org.junit.rules.ExternalResource;
import redis.embedded.Redis;
import redis.embedded.RedisServer;

import java.io.IOException;

public class RedisTestRule extends ExternalResource {
    private Redis redis;

    public RedisTestRule(int port) {
        redis = RedisServer.builder().setting("bind 127.0.0.1").port(port).build();
    }

    @Override
    protected void before() throws Throwable {
        redis.start();
        super.before();
    }

    @Override
    protected void after() {
        redis.stop();
        super.after();
    }
}
