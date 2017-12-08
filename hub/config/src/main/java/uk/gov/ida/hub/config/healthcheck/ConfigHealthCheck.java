package uk.gov.ida.hub.config.healthcheck;

import com.codahale.metrics.health.HealthCheck;

import javax.inject.Inject;

public class ConfigHealthCheck extends HealthCheck {

    @Inject
    public ConfigHealthCheck() {
    }

    public String getName() {
        return "Config Health Check";
    }

    @Override
    protected Result check() {
        return Result.healthy();
    }
}
