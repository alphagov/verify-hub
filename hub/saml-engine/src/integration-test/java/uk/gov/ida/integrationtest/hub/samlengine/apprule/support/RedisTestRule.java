package uk.gov.ida.integrationtest.hub.samlengine.apprule.support;

import org.junit.rules.ExternalResource;
import redis.embedded.Redis;
import redis.embedded.RedisServer;

import java.io.IOException;

public class RedisTestRule extends ExternalResource {
    private Redis redis;

    public RedisTestRule(int port) {
        try {
            redis = new RedisServer(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
