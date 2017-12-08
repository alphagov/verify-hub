package uk.gov.ida.stub.event.sink.healthcheck;

import com.codahale.metrics.health.HealthCheck;

public class StubEventSinkHealthCheck extends HealthCheck {

    public String getName() {
        return "EventSink Health Check";
    }

    @Override
    protected Result check() {
        return Result.healthy();
    }
}
