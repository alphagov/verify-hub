package uk.gov.ida.hub.samlengine.exceptions;

import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class IdaJsonProcessingExceptionMapperBundle implements Bundle {

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // this method intentionally left blank
    }

    @Override
    public void run(Environment environment) {
        environment.jersey().register(IdaJsonProcessingExceptionMapper.class);
    }
}
