package uk.gov.ida.hub.samlsoapproxy.runnabletasks;

import com.codahale.metrics.Counter;
import com.google.inject.Injector;
import com.google.inject.Key;
import uk.gov.ida.common.ServiceInfoConfiguration;
import uk.gov.ida.common.SessionId;
import uk.gov.ida.eventemitter.EventEmitter;
import uk.gov.ida.hub.shared.eventsink.EventSinkProxy;
import uk.gov.ida.hub.samlsoapproxy.annotations.MatchingServiceRequestExecutorBacklog;
import uk.gov.ida.hub.samlsoapproxy.domain.AttributeQueryContainerDto;
import uk.gov.ida.hub.samlsoapproxy.domain.TimeoutEvaluator;
import uk.gov.ida.hub.samlsoapproxy.proxy.HubMatchingServiceResponseReceiverProxy;

import javax.inject.Inject;

public class AttributeQueryRequestRunnableFactory {

    private Injector injector;

    @Inject
    public AttributeQueryRequestRunnableFactory(Injector injector) {
        this.injector = injector;
    }

    public Runnable create(final SessionId sessionId, final AttributeQueryContainerDto attributeQueryContainerDto) {
        return new AttributeQueryRequestRunnable(
                sessionId,
                attributeQueryContainerDto,
                injector.getInstance(ExecuteAttributeQueryRequest.class),
                injector.getInstance(Key.get(Counter.class, MatchingServiceRequestExecutorBacklog.class)),
                injector.getInstance(TimeoutEvaluator.class),
                injector.getInstance(HubMatchingServiceResponseReceiverProxy.class),
                injector.getInstance(ServiceInfoConfiguration.class),
                injector.getInstance(EventSinkProxy.class),
                injector.getInstance(EventEmitter.class));
    }
}
