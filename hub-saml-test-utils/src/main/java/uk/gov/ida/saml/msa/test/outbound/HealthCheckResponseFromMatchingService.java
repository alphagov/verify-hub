package uk.gov.ida.saml.msa.test.outbound;

import org.joda.time.DateTime;
import uk.gov.ida.saml.core.domain.IdaMatchingServiceResponse;

public class HealthCheckResponseFromMatchingService extends IdaMatchingServiceResponse {
    public HealthCheckResponseFromMatchingService(String entityId, String healthCheckReqeustId) {
        super("healthcheck-response-id", healthCheckReqeustId, entityId, DateTime.now());
    }
}

