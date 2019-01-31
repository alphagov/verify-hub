package uk.gov.ida.integrationtest.hub.samlsoapproxy.apprule.support;

import httpstub.AbstractHttpStub;
import httpstub.ReceivedRequest;
import httpstub.RecordedRequest;
import httpstub.RequestAndResponse;
import httpstub.StubHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

public class SleepyHttpStub extends AbstractHttpStub {
    private static final Logger LOG = LoggerFactory.getLogger(SleepyHttpStub.class);
    private final long sleepTime;

    public SleepyHttpStub(final long sleepTime) {
        this(RANDOM_PORT, sleepTime);
    }

    public SleepyHttpStub(final int port,
                          final long sleepTime) {
        super(port);
        this.sleepTime = sleepTime;
    }

    @Override
    public StubHandler createHandler() {
        return new SleepyHttpStub.Handler();
    }

    private class Handler extends StubHandler {

        @Override
        protected void recordRequest(RecordedRequest recordedRequest) {
            recordedRequests.add(recordedRequest);
        }

        @Override
        public Optional<RequestAndResponse> findResponse(ReceivedRequest receivedRequest) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                LOG.warn(e.getMessage());
            }
            return requestsAndResponses
                       .stream()
                       .filter(requestAndResponse -> requestAndResponse.getRequest().applies(receivedRequest))
                       .min(Comparator.comparingInt(RequestAndResponse::callCount));
        }
    }
}
